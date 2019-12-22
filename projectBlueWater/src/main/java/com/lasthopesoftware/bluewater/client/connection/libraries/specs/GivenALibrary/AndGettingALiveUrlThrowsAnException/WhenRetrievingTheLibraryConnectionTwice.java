package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary.AndGettingALiveUrlThrowsAnException;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheLibraryConnectionTwice {

	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static IConnectionProvider connectionProvider;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(2)).thenReturn(new Promise<>(library));

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library))
			.thenReturn(new Promise<>(new IOException("An error!")))
			.thenReturn(new Promise<>(firstUrlProvider));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			Promise::new,
			liveUrlProvider,
			(provider) -> new Promise<>(Collections.singletonList(new Item(5))),
			OkHttpFactory.getInstance());

		connectionProvider = new FuturePromise<>(
			libraryConnectionProvider
				.promiseLibraryConnection(new LibraryId(2))
				.updates(statuses::add)
				.eventually(
					c -> libraryConnectionProvider
						.promiseLibraryConnection(new LibraryId(2))
						.updates(statuses::add),
					e -> libraryConnectionProvider
						.promiseLibraryConnection(new LibraryId(2))
						.updates(statuses::add))).get();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(firstUrlProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.GettingView,
				BuildingConnectionStatus.BuildingSessionComplete);
	}
}
