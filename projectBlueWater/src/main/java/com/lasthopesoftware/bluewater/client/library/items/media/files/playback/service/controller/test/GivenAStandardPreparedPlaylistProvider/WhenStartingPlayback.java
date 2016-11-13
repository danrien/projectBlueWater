package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedPlaybackHandlerContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayback;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 11/12/16.
 */

public class WhenStartingPlayback {

	private IPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = mock(IPlaybackHandler.class);

		final IPromise<PositionedPlaybackHandlerContainer> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackHandlerContainer(0, playbackHandler));

		final PlaylistPlayback playlistPlayback =
			new PlaylistPlayback(new IPreparedPlaybackFileProvider() {
				@Override
				public IPromise<PositionedPlaybackHandlerContainer> promiseNextPreparedPlaybackFile(int preparedAt) {
					return positionedPlaybackHandlerContainer;
				}

				@Override
				public void close() throws IOException {

				}
			}, 0);
	}

	@Test
	public void thenPlaybackIsBegun() {
		verify(playbackHandler, times(1)).promisePlayback();
	}
}
