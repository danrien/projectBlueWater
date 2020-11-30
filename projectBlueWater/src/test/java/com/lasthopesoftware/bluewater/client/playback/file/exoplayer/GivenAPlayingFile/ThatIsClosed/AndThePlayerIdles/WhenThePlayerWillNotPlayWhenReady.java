package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsClosed.AndThePlayerIdles;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenThePlayerWillNotPlayWhenReady {
	private static final Collection<Player.EventListener> eventListeners = new ArrayList<>();
	private static final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListeners.add(invocation.getArgument(0));
			return null;
		}).when(mockExoPlayer).addListener(any());

		ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);

		new FuturePromise<>(exoPlayerPlaybackHandler.promisePlayback()).get(1, TimeUnit.SECONDS);

		exoPlayerPlaybackHandler.close();

		Stream.of(eventListeners)
				.forEach(e -> e.onPlayerStateChanged(false, Player.STATE_IDLE));
	}

	@Test
	public void thenPlaybackIsNotRestarted() {
		verify(mockExoPlayer, times(1)).setPlayWhenReady(true);
	}
}