package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary.AndAConnectionIsNotStillAlive;

import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.specs.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.resources.specs.BroadcastRecorder;
import com.lasthopesoftware.resources.specs.ScopedLocalBroadcastManagerBuilder;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingConnection;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingView;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.buildSessionBroadcastStatus;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingATestedSessionConnection extends AndroidContext {

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();
	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;
	private static IConnectionProvider secondConnectionProvider;

	@Override
	public void before() throws ExecutionException, InterruptedException, IllegalAccessException, InstantiationException, InvocationTargetException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(new Promise<>(library));

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManagerBuilder.newScopedBroadcastManager(RuntimeEnvironment.application);
		localBroadcastManager.registerReceiver(
			broadcastRecorder,
			new IntentFilter(SessionConnection.buildSessionBroadcast));

		final TestConnections testConnections = mock(TestConnections.class);
		when(testConnections.promiseIsConnectionPossible(any()))
				.thenReturn(new Promise<>(false));

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			fakeSelectedLibraryProvider.selectedLibraryId = 2;
			final SessionConnection sessionConnection = new SessionConnection(
				localBroadcastManager,
				fakeSelectedLibraryProvider,
				libraryProvider,
				(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
				Promise::new,
				liveUrlProvider,
				testConnections,
				OkHttpFactory.getInstance());

			connectionProvider = new FuturePromise<>(sessionConnection.promiseSessionConnection()).get();
			secondConnectionProvider = new FuturePromise<>(sessionConnection.promiseTestedSessionConnection()).get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider.getUrlProvider()).isEqualTo(connectionProvider.getUrlProvider());
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(Stream.of(broadcastRecorder.recordedIntents).map(i -> i.getIntExtra(buildSessionBroadcastStatus, -1)).toList())
			.containsExactly(
				GettingLibrary,
				BuildingConnection,
				GettingView,
				BuildingSessionComplete,
				GettingLibrary,
				BuildingConnection,
				BuildingSessionComplete);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		int selectedLibraryId;

		@Override
		public int getSelectedLibraryId() {
			return selectedLibraryId;
		}
	}
}
