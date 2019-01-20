package com.lasthopesoftware.bluewater.client.stored.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester;
import com.lasthopesoftware.bluewater.client.library.BrowseLibraryActivity;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.broadcasts.ScanMediaFileBroadcaster;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.FileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.read.StorageReadPermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.StorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.client.stored.library.sync.factory.LibrarySyncHandlerFactory;
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification;
import com.lasthopesoftware.bluewater.client.stored.service.receivers.*;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.bluewater.client.stored.sync.SynchronizeStoredFiles;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StoredSyncService extends Service implements PostSyncNotification {

	private static final Logger logger = LoggerFactory.getLogger(StoredSyncService.class);

	private static final String doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "doSyncAction");
	private static final String cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "cancelSyncAction");

	private static final int notificationId = 23;

	private static final int buildConnectionTimeoutTime = 10000;

	private static volatile Disposable syncDisposable;
	private PowerManager.WakeLock wakeLock;

	public static boolean isSyncRunning() {
		return syncDisposable != null;
	}

	public static void doSync(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(doSyncAction);

		safelyStartService(context, intent);
	}

	public static void cancelSync(Context context) {
		final Intent intent = new Intent(context, StoredSyncService.class);
		intent.setAction(cancelSyncAction);

		safelyStartService(context, intent);
	}

	private static void safelyStartService(Context context, Intent intent) {
		try {
			context.startService(intent);
		} catch (IllegalStateException e) {
			logger.warn("An illegal state exception occurred while trying to start the service", e);
		} catch (SecurityException e) {
			logger.warn("A security exception occurred while trying to start the service", e);
		}
	}

	private final AbstractSynchronousLazy<BroadcastReceiver> onWifiStateChangedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		protected final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (!IoCommon.isWifiConnected(context)) cancelSync(StoredSyncService.this);
				}
			};
		}
	};

	private final AbstractSynchronousLazy<BroadcastReceiver> onPowerDisconnectedReceiver = new AbstractSynchronousLazy<BroadcastReceiver>() {
		@Override
		public final BroadcastReceiver create() {
			return new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					cancelSync(StoredSyncService.this);
				}
			};
		}
	};

	private final CreateAndHold<String> lazyActiveNotificationChannelId = new AbstractSynchronousLazy<String>() {
		@Override
		protected String create() {
			final NotificationChannelActivator notificationChannelActivator = new NotificationChannelActivator(notificationManagerLazy.getObject());

			return notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());
		}
	};

	private final AbstractSynchronousLazy<Intent> browseLibraryIntent = new AbstractSynchronousLazy<Intent>() {
		@Override
		protected final Intent create() {
			final Intent browseLibraryIntent = new Intent(StoredSyncService.this, BrowseLibraryActivity.class);
			browseLibraryIntent.setAction(BrowseLibraryActivity.showDownloadsAction);
			browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			return browseLibraryIntent;
		}
	};

	private final CreateAndHold<NotificationManager> notificationManagerLazy = new AbstractSynchronousLazy<NotificationManager>() {
		@Override
		protected NotificationManager create() {
			return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
	};

	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new AbstractSynchronousLazy<ChannelConfiguration>() {
		@Override
		protected ChannelConfiguration create() {
			return new SharedChannelProperties(StoredSyncService.this);
		}
	};

	private final CreateAndHold<LocalBroadcastManager> lazyBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));

	private final CreateAndHold<UrlScanner> lazyUrlScanner = new AbstractSynchronousLazy<UrlScanner>() {
		@Override
		protected UrlScanner create() {
			final OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(buildConnectionTimeoutTime, TimeUnit.MILLISECONDS)
				.build();

			return new UrlScanner(
				new ConnectionTester(),
				new ServerLookup(new ServerInfoXmlRequest(client)),
				OkHttpFactory.getInstance());
		}
	};

	private final CreateAndHold<IStoredFileAccess> lazyStoredFileAccess = new Lazy<>(() -> new StoredFileAccess(this, new StoredFilesCollection(this)));

	private final CreateAndHold<IStorageReadPermissionArbitratorForOs> lazyReadPermissionArbitratorForOs = new Lazy<>(() -> new ExternalStorageReadPermissionsArbitratorForOs(this));

	private final CreateAndHold<ILibraryProvider> lazyLibraryRepository = new Lazy<>(() -> new LibraryRepository(this));

	private final CreateAndHold<SynchronizeStoredFiles> lazyStoredFilesSynchronization = new AbstractSynchronousLazy<SynchronizeStoredFiles>() {
		@Override
		protected SynchronizeStoredFiles create() {
			final StoredSyncService storedSyncService = StoredSyncService.this;

			return new StoredFileSynchronization(
				lazyLibraryRepository.getObject(),
				lazyBroadcastManager.getObject(),
				lazyUrlScanner.getObject(),
				new LibrarySyncHandlerFactory(
					lazyStoredFileAccess.getObject(),
					storedSyncService,
					lazyReadPermissionArbitratorForOs.getObject(),
					new SyncDirectoryLookup(new PublicDirectoryLookup(storedSyncService), new PrivateDirectoryLookup(storedSyncService)),
					new StoredFileSystemFileProducer(),
					new ServiceFileUriQueryParamsProvider(),
					new FileReadPossibleArbitrator(),
					new FileWritePossibleArbitrator(),
					new FileStreamWriter()),
				storedSyncService);
		}
	};

	private final CreateAndHold<ReceiveStoredFileEvent[]> lazyStoredFileEventReceivers = new AbstractSynchronousLazy<ReceiveStoredFileEvent[]>() {
		@Override
		protected ReceiveStoredFileEvent[] create() {
			final StoredSyncService storedSyncService = StoredSyncService.this;
			final StoredFileDownloadingNotifier storedFileDownloadingNotifier = new StoredFileDownloadingNotifier(
				lazyStoredFileAccess.getObject(),
				lazyLibraryRepository.getObject(),
				lazyUrlScanner.getObject(),
				storedSyncService,
				storedSyncService);

			final StoredFileMediaScannerNotifier storedFileMediaScannerNotifier = new StoredFileMediaScannerNotifier(
				lazyStoredFileAccess.getObject(),
				new ScanMediaFileBroadcaster(StoredSyncService.this));

			final StoredFileReadPermissionsReceiver storedFileReadPermissionsReceiver = new StoredFileReadPermissionsReceiver(
				lazyReadPermissionArbitratorForOs.getObject(),
				new StorageReadPermissionsRequestedBroadcaster(lazyBroadcastManager.getObject()),
				lazyStoredFileAccess.getObject());

			final StoredFileWritePermissionsReceiver storedFileWritePermissionsReceiver = new StoredFileWritePermissionsReceiver(
				new ExternalStorageWritePermissionsArbitratorForOs(StoredSyncService.this),
				new StorageWritePermissionsRequestedBroadcaster(lazyBroadcastManager.getObject()),
				lazyStoredFileAccess.getObject());

			return new ReceiveStoredFileEvent[] {
				storedFileDownloadingNotifier,
				storedFileMediaScannerNotifier,
				storedFileReadPermissionsReceiver,
				storedFileWritePermissionsReceiver
			};
		}
	};

	private volatile List<BroadcastReceiver> broadcastReceivers = new ArrayList<>();

	@Override
	public void onCreate() {
		super.onCreate();


		final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService.class, "wakeLock"));
		wakeLock.acquire();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) return START_NOT_STICKY;

		final String action = intent.getAction();

		if (cancelSyncAction.equals(action)) {
			stopSelf();
			return START_NOT_STICKY;
		}

		if (!doSyncAction.equals(action)) return START_REDELIVER_INTENT;

		if (isSyncRunning() || !isDeviceStateValidForSync()) return START_NOT_STICKY;

		if (!lazyStoredFileEventReceivers.isCreated()) {
			for (final ReceiveStoredFileEvent receiveStoredFileEvent : lazyStoredFileEventReceivers.getObject()) {
				final StoredFileBroadcastReceiver broadcastReceiver = new StoredFileBroadcastReceiver(receiveStoredFileEvent);
				if (!broadcastReceivers.add(broadcastReceiver)) continue;

				lazyBroadcastManager.getObject().registerReceiver(
					broadcastReceiver,
					Stream.of(receiveStoredFileEvent.acceptedEvents()).reduce(new IntentFilter(), (i, e) -> {
						i.addAction(e);
						return i;
					}));
			}
		}

		syncDisposable = lazyStoredFilesSynchronization.getObject()
			.streamFileSynchronization()
			.subscribe(this::stopSelf, e -> stopSelf());

		return START_NOT_STICKY;
	}

	private boolean isDeviceStateValidForSync() {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean isSyncOnWifiOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false);
		if (isSyncOnWifiOnly) {
			if (!IoCommon.isWifiConnected(this)) return false;

			registerReceiver(onWifiStateChangedReceiver.getObject(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}

		final boolean isSyncOnPowerOnly = sharedPreferences.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false);
		if (isSyncOnPowerOnly) {
			if (!IoCommon.isPowerConnected(this)) return false;

			registerReceiver(onPowerDisconnectedReceiver.getObject(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		}

		return true;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);

		if (syncDisposable != null) {
			syncDisposable.dispose();
			syncDisposable = null;
		}

		if (lazyBroadcastManager.isCreated()) {
			while (!broadcastReceivers.isEmpty()) {
				final BroadcastReceiver receiver = broadcastReceivers.remove(0);
				lazyBroadcastManager.getObject().unregisterReceiver(receiver);
			}
		}

		if (onWifiStateChangedReceiver.isCreated())
			unregisterReceiver(onWifiStateChangedReceiver.getObject());

		if (onPowerDisconnectedReceiver.isCreated())
			unregisterReceiver(onPowerDisconnectedReceiver.getObject());

		wakeLock.release();

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	private final Lazy<IBinder> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	@Override
	public void notify(String notificationText) {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, lazyActiveNotificationChannelId.getObject());
		notifyBuilder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files));
		if (notificationText != null)
			notifyBuilder.setContentText(notificationText);
		notifyBuilder.setContentIntent(PendingIntent.getActivity(this, 0, browseLibraryIntent.getObject(), 0));

		notifyBuilder.setOngoing(true);

		notifyBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

		final Notification syncNotification = notifyBuilder.build();

		startForeground(notificationId, syncNotification);
	}
}
