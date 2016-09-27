package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import android.media.MediaPlayer;

/**
 * Created by david on 9/21/16.
 */

public class MediaPlayerErrorData {
	private final MediaPlayer mediaPlayer;
	public final int what;
	public final int extra;

	public MediaPlayerErrorData(MediaPlayer mediaPlayer, int what, int extra) {
		this.mediaPlayer = mediaPlayer;
		this.what = what;
		this.extra = extra;
	}
}
