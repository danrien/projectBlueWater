package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.progress.ReadFileProgress;

import org.joda.time.Duration;

public class ExoPlayerFileProgressReader implements ReadFileProgress {

	private final ExoPlayer exoPlayer;

	private Duration fileProgress = Duration.ZERO;

	public ExoPlayerFileProgressReader(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
	}

	@Override
	public synchronized Duration getFileProgress() {
		if (!exoPlayer.getPlayWhenReady()) return fileProgress;

		return fileProgress = Duration.millis(exoPlayer.getCurrentPosition());
	}
}
