package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.io.IOException;

public class LoadingExoPlayer
implements
	IBufferingPlaybackFile,
	MessengerOperator<IBufferingPlaybackFile>,
	MediaSourceEventListener
{

	private final CreateAndHold<Promise<IBufferingPlaybackFile>> bufferingPlaybackFilePromise = new AbstractSynchronousLazy<Promise<IBufferingPlaybackFile>>() {
		@Override
		protected Promise<IBufferingPlaybackFile> create() throws Exception {
			return new Promise<>((MessengerOperator<IBufferingPlaybackFile>) LoadingExoPlayer.this);
		}
	};

	private Messenger<IBufferingPlaybackFile> messenger;
	private boolean isTransferComplete;
	private Exception loadError;

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return bufferingPlaybackFilePromise.getObject();
	}

	@Override
	public void send(Messenger<IBufferingPlaybackFile> messenger) {
		this.messenger = messenger;
		if (isTransferComplete)
			messenger.sendResolution(this);
		if (loadError != null)
			messenger.sendRejection(loadError);
	}

	@Override
	public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {

	}

	@Override
	public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
		isTransferComplete = true;
		if (messenger != null)
			messenger.sendResolution(this);
	}

	@Override
	public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

	}

	@Override
	public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
		loadError = error;
		if (messenger != null)
			messenger.sendRejection(error);
	}

	@Override
	public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {

	}

	@Override
	public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {

	}
}
