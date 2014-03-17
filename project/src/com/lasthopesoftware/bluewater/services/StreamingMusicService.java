/**
 * 
 */
package com.lasthopesoftware.bluewater.services;


import java.util.HashSet;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrFilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.JrPlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileProperties;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.receivers.RemoteControlReceiver;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements
	OnAudioFocusChangeListener, 
	OnNowPlayingChangeListener, 
	OnNowPlayingStartListener,
	OnNowPlayingStopListener, 
	OnPlaylistStateControlErrorListener
{
	/* String constant actions */
	private static final String ACTION_START = "com.lasthopesoftware.bluewater.ACTION_START";
	private static final String ACTION_PLAY = "com.lasthopesoftware.bluewater.ACTION_PLAY";
	private static final String ACTION_PAUSE = "com.lasthopesoftware.bluewater.ACTION_PAUSE";
	private static final String ACTION_SYSTEM_PAUSE = "com.lasthopesoftware.bluewater.ACTION_SYSTEM_PAUSE";
	private static final String ACTION_STOP_WAITING_FOR_CONNECTION = "com.lasthopesoftware.bluewater.ACTION_STOP_WAITING_FOR_CONNECTION";
	private static final String ACTION_INITIALIZE_PLAYLIST = "com.lasthopesoftware.bluewater.ACTION_INITIALIZE_PLAYLIST";
	
	/* Bag constants */
	private static final String BAG_FILE_KEY = "com.lasthopesoftware.bluewater.bag.FILE_KEY";
	private static final String BAG_PLAYLIST = "com.lasthopesoftware.bluewater.bag.FILE_PLAYLIST";
	private static final String BAG_START_POS = "com.lasthopesoftware.bluewater.bag.START_POS";
	
	/* Miscellaneous programming related string constants */
	private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";
	private static final String WIFI_LOCK_SVC_NAME =  "project_blue_water_svc_lock";
	
	
	private static int mId = 42;
	private static int mStartId;
	private WifiLock mWifiLock = null;
	private NotificationManager mNotificationMgr;
	private Context thisContext;
	private AudioManager mAudioManager;
	private ComponentName mRemoteControlReceiver;
	
	// State dependent static variables
	private static String mPlaylistString;
	private static JrPlaylistController mPlaylistController;
	
	private static Object syncObject = new Object();
	
	private static HashSet<OnNowPlayingChangeListener> mOnStreamingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private static HashSet<OnNowPlayingStartListener> mOnStreamingStartListeners = new HashSet<OnNowPlayingStartListener>();
	private static HashSet<OnNowPlayingStopListener> mOnStreamingStopListeners = new HashSet<OnNowPlayingStopListener>();
	
	/* Begin streamer intent helpers */
	public static void initializePlaylist(Context context, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}
	
	public static void initializePlaylist(Context context, int startFileKey, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}
	
	public static void initializePlaylist(Context context, int startFileKey, int startPos, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, startPos);
		context.startService(svcIntent);
	}
	
	public static void streamMusic(Context context, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(Context context, int startFileKey, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(Context context, int startFileKey, int startPos, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, startPos);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(Context context, int startFileKey) { 
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void streamMusic(Context context, int startFileKey, int startPos) { 
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		svcIntent.putExtra(BAG_START_POS, startPos);
		context.startService(svcIntent);
	}
	
	public static void play(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY);
		context.startService(svcIntent);
	}
	
	public static void pause(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PAUSE);
		context.startService(svcIntent);
	}
	
	public static void next(Context context) {
		JrFilePlayer currentFilePlayer = mPlaylistController.getCurrentFilePlayer();
		if (currentFilePlayer == null) return;
		JrFile nextFile = currentFilePlayer.getFile().getNextFile();
		if (nextFile == null) return;
		Intent svcIntent = new Intent(currentFilePlayer.isPlaying() ? StreamingMusicService.ACTION_START : StreamingMusicService.ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, nextFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void previous(Context context) {
		JrFilePlayer currentFilePlayer = mPlaylistController.getCurrentFilePlayer();
		if (currentFilePlayer == null) return;
		JrFile previousFile = currentFilePlayer.getFile().getPreviousFile();
		if (previousFile == null) return;
		Intent svcIntent = new Intent(currentFilePlayer.isPlaying() ? StreamingMusicService.ACTION_START : StreamingMusicService.ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, previousFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void setIsRepeating(Context context, boolean isRepeating) {
		JrSession.GetLibrary(context).setRepeating(isRepeating);
		JrSession.SaveSession(context);
		if (mPlaylistController != null) mPlaylistController.setIsRepeating(isRepeating);
	}
	
	/* End streamer intent helpers */
	
	/* Begin Events */
	public static void addOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		mOnStreamingChangeListeners.add(listener);
	}

	public static void addOnStreamingStartListener(OnNowPlayingStartListener listener) {
		mOnStreamingStartListeners.add(listener);
	}
	
	public static void addOnStreamingStopListener(OnNowPlayingStopListener listener) {
		mOnStreamingStopListeners.add(listener);
	}
		
	public static void removeOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		synchronized(syncObject) {
			if (mOnStreamingChangeListeners.contains(listener))
				mOnStreamingChangeListeners.remove(listener);
		}
	}

	public static void removeOnStreamingStartListener(OnNowPlayingStartListener listener) {
		synchronized(syncObject) {
			if (mOnStreamingStartListeners.contains(listener))
				mOnStreamingStartListeners.remove(listener);
		}
	}
	
	public static void removeOnStreamingStopListener(OnNowPlayingStopListener listener) {
		synchronized(syncObject) {
			if (mOnStreamingStopListeners.contains(listener))
				mOnStreamingStopListeners.remove(listener);
		}
	}
	
	private void throwChangeEvent(JrPlaylistController controller, JrFilePlayer filePlayer) {
		synchronized(syncObject) {
			for (OnNowPlayingChangeListener onChangeListener : mOnStreamingChangeListeners)
				onChangeListener.onNowPlayingChange(controller, filePlayer);
		}
	}

	private void throwStartEvent(JrPlaylistController controller, JrFilePlayer filePlayer) {
		synchronized(syncObject) {
			for (OnNowPlayingStartListener onStartListener : mOnStreamingStartListeners)
				onStartListener.onNowPlayingStart(controller, filePlayer);
		}
	}
	
	private void throwStopEvent(JrPlaylistController controller, JrFilePlayer filePlayer) {
		synchronized(syncObject) {
			for (OnNowPlayingStopListener onStopListener : mOnStreamingStopListeners)
				onStopListener.onNowPlayingStop(controller, filePlayer);
		}
	}
	/* End Events */
		
	public static JrPlaylistController getPlaylistController() {
		return mPlaylistController;
	}
	
	public StreamingMusicService() {
		super();
		thisContext = this;
	}
	
	private void startPlaylist(String playlistString, int fileKey, int filePos) {
		if (playlistString == null) return;
		// If the playlist has changed, change that
		if (mPlaylistController == null || !playlistString.equals(mPlaylistString)) {
			initializePlaylist(playlistString);
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		builder.setContentTitle("Starting Music Streamer");
        startForeground(mId, builder.build());
        
        mPlaylistController.startAt(fileKey, filePos);
	}
		
	private void initializePlaylist(String playlistString) {
		mPlaylistString = playlistString;
		
		JrSession.GetLibrary(thisContext).setSavedTracksString(mPlaylistString);
		JrSession.SaveSession(thisContext);
		
		if (mPlaylistController != null) {
			mPlaylistController.pause();
			mPlaylistController.release();
		}
		
		mPlaylistController = new JrPlaylistController(thisContext, playlistString);
		mPlaylistController.setIsRepeating(JrSession.GetLibrary(thisContext).isRepeating());
		mPlaylistController.addOnNowPlayingChangeListener(this);
		mPlaylistController.addOnNowPlayingStopListener(this);
		mPlaylistController.addOnPlaylistStateControlErrorListener(this);
		mPlaylistController.addOnNowPlayingStartListener(this);
	}
	

	private void initializePlaylist(String playlistString, int fileKey, int filePos) {
		initializePlaylist(playlistString);
		mPlaylistController.seekTo(fileKey, filePos);
	}
	
	private void pausePlayback(boolean isUserInterrupted) {
		if (mPlaylistController != null) {
			if (mPlaylistController.isPlaying()) {
				if (isUserInterrupted) mAudioManager.abandonAudioFocus(this);
				mPlaylistController.pause();
			}
		}
		stopNotification();
		if (isUserInterrupted) stopSelfResult(mStartId);
	}
	
	private void buildErrorNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		Intent intent = new Intent(ACTION_STOP_WAITING_FOR_CONNECTION);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);
		
		final CharSequence waitingText = getText(R.string.lbl_waiting_for_connection);
		builder.setContentTitle(waitingText);
		builder.setTicker(waitingText);
		builder.setSubText(getText(R.string.lbl_click_to_cancel));
		mNotificationMgr.notify(mId, builder.build());
		PollConnectionTask checkConnection = PollConnectionTask.Instance.get();
		
		checkConnection.addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				mNotificationMgr.cancelAll();
				if (result == Boolean.FALSE) return;
				Library library = JrSession.GetLibrary(thisContext);
				if (library != null)
					streamMusic(thisContext, library.getNowPlayingId(), library.getNowPlayingProgress(), library.getSavedTracksString());							
			}
		});
		
		checkConnection.startPolling();
	}
	
	private void stopNotification() {
		stopForeground(true);
		mNotificationMgr.cancel(mId);
	}
	
	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		mStartId = startId;
		
		if (PollConnectionTask.Instance.get().isRunning()) return START_NOT_STICKY;
		
		if (intent != null) {
			// 3/5 times it's going to be this so let's see if we can get
			// some improved prefetching by the processor
			String action = intent.getAction(); 
			if (action.equals(ACTION_START)) {
				startPlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, 0));
	        } else if (action.equals(ACTION_INITIALIZE_PLAYLIST)) {
	        	initializePlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, 0));
	        } else if (mPlaylistController != null) {
	        	// These actions can only occur if mPlaylist and the PlayingFile are not null
	        	if (action.equals(ACTION_PAUSE)) {
	        		pausePlayback(true);
		        } else if (action.equals(ACTION_PLAY) && mPlaylistController != null) {
		    		startPlaylist(mPlaylistString, JrSession.GetLibrary(thisContext).getNowPlayingId(), JrSession.GetLibrary(thisContext).getNowPlayingProgress());
		        }
	        } else if (action.equals(ACTION_STOP_WAITING_FOR_CONNECTION)) {
	        	PollConnectionTask.Instance.get().stopPolling();
	        }
		} else if (!JrSession.isActive()) {
			if (JrSession.GetLibrary(thisContext) != null) pausePlayback(true);
		}
		return START_NOT_STICKY;
	}
	
	@Override
    public void onCreate() {
		mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	@Override
	public boolean onPlaylistStateControlError(JrPlaylistController controller, JrFilePlayer filePlayer) {
		buildErrorNotification();

		return true;
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
        	if (JrSession.GetLibrary(thisContext) != null && !JrSession.isActive()) return;
        	
        	if (mPlaylistController != null) {
        		mPlaylistController.setVolume(1.0f);
        	        	
	        	if (!mPlaylistController.isPlaying()) {
	        		Library library = JrSession.GetLibrary(thisContext);
	        		startPlaylist(library.getSavedTracksString(), library.getNowPlayingId(), library.getNowPlayingProgress());
	        	}
        	}
        	
            return;
		}
		
		if (mPlaylistController == null) return;
		
	    switch (focusChange) {
        	// Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS:
	        	if (mPlaylistController.isPlaying()) pausePlayback(true);
	        // Lost focus but it will be regained... cannot release resources
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	        	if (mPlaylistController.isPlaying()) pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (mPlaylistController.isPlaying()) mPlaylistController.setVolume(0.1f);
	            return;
	    }
	}
	
	@Override
	public void onNowPlayingStop(JrPlaylistController controller, JrFilePlayer filePlayer) {
		JrSession.GetLibrary(thisContext).setNowPlayingId(filePlayer.getFile().getKey());
		JrSession.GetLibrary(thisContext).setNowPlayingProgress(filePlayer.getCurrentPosition());
		JrSession.SaveSession(thisContext);
		
		stopNotification();
		
		mAudioManager.abandonAudioFocus(this);
		// release the wifilock if we still have it
		if (mWifiLock != null) {
			if (mWifiLock.isHeld()) mWifiLock.release();
			mWifiLock = null;
		}
		
		throwStopEvent(controller, filePlayer);
	}

	@Override
	public void onNowPlayingChange(JrPlaylistController controller, JrFilePlayer filePlayer) {
		JrSession.GetLibrary(thisContext).setNowPlayingId(filePlayer.getFile().getKey());
		JrSession.GetLibrary(thisContext).setNowPlayingProgress(filePlayer.getCurrentPosition());
		JrSession.SaveSession(thisContext);
		throwChangeEvent(controller, filePlayer);
	}
	

	@Override
	public void onNowPlayingStart(JrPlaylistController controller, JrFilePlayer filePlayer) {
		final JrFile playingFile = filePlayer.getFile();
		// Start playback immediately
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		mRemoteControlReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
		mAudioManager.registerMediaButtonEventReceiver(mRemoteControlReceiver);
		
		// Set the notification area
		Intent viewIntent = new Intent(this, ViewNowPlaying.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_SVC_NAME);
        mWifiLock.acquire();
		
		final SimpleTask<Void, Void, String> getNotificationPropertiesTask = new SimpleTask<Void, Void, String>();
		getNotificationPropertiesTask.addOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				owner.setResult(playingFile.getProperty("Artist") + " - " + playingFile.getValue());
			}
		});
		getNotificationPropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				NotificationCompat.Builder builder = new NotificationCompat.Builder(thisContext);
		        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
				builder.setOngoing(true);
				builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)));
				builder.setContentText(result == null ? "Error getting file properties." : result);
				builder.setContentIntent(pi);
				mNotificationMgr.notify(mId, builder.build());
			}
		});
		
		getNotificationPropertiesTask.execute();
		
		final SimpleTask<Void, Void, SparseArray<Object>> getBtPropertiesTask = new SimpleTask<Void, Void, SparseArray<Object>>();
		getBtPropertiesTask.addOnExecuteListener(new OnExecuteListener<Void, Void, SparseArray<Object>>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, SparseArray<Object>> owner, Void... params) throws Exception {
				SparseArray<Object> result = new SparseArray<Object>(4);
				result.put(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingFile.getProperty(JrFileProperties.ARTIST));
				result.put(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingFile.getProperty(JrFileProperties.ALBUM));
				result.put(MediaMetadataRetriever.METADATA_KEY_TITLE, playingFile.getValue());
				result.put(MediaMetadataRetriever.METADATA_KEY_DURATION, Long.valueOf(playingFile.getDuration()));
				owner.setResult(result);
			}
		});
		getBtPropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, SparseArray<Object>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, SparseArray<Object>> owner, SparseArray<Object> result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				// build the PendingIntent for the remote control client
				Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
				mediaButtonIntent.setComponent(mRemoteControlReceiver);
				PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(thisContext, 0, mediaButtonIntent, 0);
				// create and register the remote control client
				RemoteControlClient remoteControlClient = new RemoteControlClient(mediaPendingIntent);
				remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
				remoteControlClient.setTransportControlFlags(
						RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
	                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
	                    RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
	                    RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
	                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);
				MetadataEditor metaData = remoteControlClient.editMetadata(true);
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, (String)result.get(MediaMetadataRetriever.METADATA_KEY_ARTIST));
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, (String)result.get(MediaMetadataRetriever.METADATA_KEY_ALBUM));
				metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, (String)result.get(MediaMetadataRetriever.METADATA_KEY_TITLE));				
				metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, (Long)result.get(MediaMetadataRetriever.METADATA_KEY_DURATION));
				metaData.apply();
				
				final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
				pebbleIntent.putExtra("artist", (String)result.get(MediaMetadataRetriever.METADATA_KEY_ARTIST));
				pebbleIntent.putExtra("album", (String)result.get(MediaMetadataRetriever.METADATA_KEY_ALBUM));
				pebbleIntent.putExtra("track", (String)result.get(MediaMetadataRetriever.METADATA_KEY_TITLE));
			    
			    sendBroadcast(pebbleIntent);
				
				mAudioManager.registerRemoteControlClient(remoteControlClient);
			}
		});
		getBtPropertiesTask.execute();
		
		throwStartEvent(controller, filePlayer);
	}
	
	@Override
	public void onDestroy() {
		JrSession.SaveSession(this);
		
		stopNotification();
		
		if (mPlaylistController != null) {
			mPlaylistController.release();
			mPlaylistController = null;
		}
		
		mPlaylistString = null;
	}

	/* End Event Handlers */
	
	/* Begin Binder Code */
	
	public class StreamingMusicServiceBinder extends Binder {
        StreamingMusicService getService() {
            return StreamingMusicService.this;
        }
    }

    private final IBinder mBinder = new StreamingMusicServiceBinder();
	/* End Binder Code */
}
