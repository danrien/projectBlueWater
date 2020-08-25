package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaybackEngine;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenRemovingFilesBeforeTheCurrentlyPlayingFile {

	private static CompletingFileQueueProvider fileQueueProvider = spy(new CompletingFileQueueProvider());
	private static final Library library = new Library();
	private static NowPlaying nowPlaying;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		library.setId(1);
		library.setSavedTracksString(new FuturePromise<>(FileStringListUtilities.promiseSerializedFileStringList(Arrays.asList(
			new ServiceFile(1),
			new ServiceFile(2),
			new ServiceFile(3),
			new ServiceFile(4),
			new ServiceFile(5)))).get());
		library.setNowPlayingId(2);

		final ISpecificLibraryProvider libraryProvider = () -> new Promise<>(library);
		final ILibraryStorage libraryStorage = new ILibraryStorage() {
			@NotNull
			@Override
			public Promise<Library> saveLibrary(@NotNull Library library) {
				return new Promise<>(library);
			}

			@NotNull
			@Override
			public Promise<?> removeLibrary(@NotNull Library library) {
				return Promise.empty();
			}
		};

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = mock(IFilePropertiesContainerRepository.class);
		when(filePropertiesContainerRepository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(5))))
			.thenReturn(new FilePropertiesContainer(1, new HashMap<String, String>() {{
				put(KnownFileProperties.DURATION, "100");
			}}));

		final NowPlayingRepository repository = new NowPlayingRepository(libraryProvider, libraryStorage);

		final PlaybackEngine playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Collections.singletonList(fileQueueProvider),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		new FuturePromise<>(playbackEngine.resume()).get(1, TimeUnit.SECONDS);

		final ResolvablePlaybackHandler resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		resolvablePlaybackHandler.setCurrentPosition(35);

		new FuturePromise<>(playbackEngine.removeFileAtPosition(0)).get(1, TimeUnit.SECONDS);

		resolvablePlaybackHandler.setCurrentPosition(92);

		new FuturePromise<>(playbackEngine.pause()).get();

		nowPlaying = new FuturePromise<>(repository.getNowPlaying()).get();
	}

	@Test
	public void thenTheCurrentlyPlayingFileShifts() {
		assertThat(library.getNowPlayingId()).isEqualTo(1);
	}

	@Test
	public void thenTheFileQueueIsShifted() {
		verify(fileQueueProvider, times(2)).provideQueue(any(), intThat(i -> i == 2));
	}

	@Test
	public void thenTheCurrentlyPlayingFileStillTracksFileProgress() {
		assertThat(nowPlaying.filePosition).isEqualTo(92);
	}
}
