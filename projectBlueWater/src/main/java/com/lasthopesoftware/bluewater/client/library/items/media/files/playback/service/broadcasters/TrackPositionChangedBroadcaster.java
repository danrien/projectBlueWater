package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;

import io.reactivex.functions.Consumer;

/**
 * Created by david on 1/29/17.
 */

public class TrackPositionChangedBroadcaster implements Consumer<Integer> {
	private ILazy<LocalBroadcastManager> lazyLocalBroadcastManager;
	private final PositionedPlaybackFile positionedPlaybackFile;

	public TrackPositionChangedBroadcaster(Context context, PositionedPlaybackFile positionedPlaybackFile) {
		lazyLocalBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(context));
		this.positionedPlaybackFile = positionedPlaybackFile;
	}

	@Override
	public void accept(Integer newPosition) throws Exception {
		final Intent trackPositionChangedIntent = new Intent(onTrackPositionChanged);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, newPosition.intValue());
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, positionedPlaybackFile.getPlaybackHandler().getDuration());

		lazyLocalBroadcastManager.getObject().sendBroadcast(trackPositionChangedIntent);
	}

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionChangedBroadcaster.class);

	public static final String onTrackPositionChanged = magicPropertyBuilder.buildProperty("onTrackPositionChange");

	public static class TrackPositionChangedParameters {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionChangedParameters.class);

		public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
	}
}
