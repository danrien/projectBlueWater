package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/**
 * Created by david on 12/17/16.
 */
public class PlaylistPlayerManager implements IPlaylistPlayerManager, Closeable {

	private final IPositionedFileQueueProvider playbackQueuesProvider;
	private final IPlaybackPreparerTaskFactory playbackPreparerTaskFactory;
	private List<IFile> playlist;
	private PlaylistPlayer playlistPlayer;
	private ObservableEmitter<PositionedPlaybackFile> emitter;

	public PlaylistPlayerManager(IPositionedFileQueueProvider playbackQueuesProvider, IPlaybackPreparerTaskFactory playbackPreparerTaskFactory) {
		this.playbackQueuesProvider = playbackQueuesProvider;
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
	}

	@Override
	public IPlaylistPlayerManager startAsCompletable(List<IFile> playlist, int playlistStart, int fileStart) throws IOException {
		this.playlist = playlist;
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackPreparerTaskFactory, playbackQueuesProvider.getCompletableQueue(playlist, playlistStart));
		return getNewPlaylistPlayer(playbackFileQueue, fileStart);
	}

	@Override
	public IPlaylistPlayerManager startAsCyclical(List<IFile> playlist, int playlistStart, int fileStart) throws IOException {
		this.playlist = playlist;
		final IPreparedPlaybackFileQueue playbackFileQueue = new PreparedPlaybackQueue(playbackPreparerTaskFactory, playbackQueuesProvider.getCyclicalQueue(playlist, playlistStart));
		return getNewPlaylistPlayer(playbackFileQueue, fileStart);
	}

	@Override
	public IPlaylistPlayerManager continueAsCompletable() {
		Observable
			.create(playlistPlayer)
			.firstElement()
			.subscribe(
				f -> startAsCompletable(this.playlist, f.getPosition(), 0),
				e -> {
					if (emitter != null)
						emitter.onError(e);
				});

		return this;
	}

	@Override
	public IPlaylistPlayerManager continueAsCyclical() {
		Observable
			.create(playlistPlayer)
			.firstElement()
			.subscribe(
				f -> startAsCyclical(this.playlist, f.getPosition(), 0),
				e -> {
					if (emitter != null)
						emitter.onError(e);
				},
				() -> startAsCyclical(this.playlist, 0, 0));

		return this;
	}

	private IPlaylistPlayerManager getNewPlaylistPlayer(IPreparedPlaybackFileQueue preparedPlaybackFileQueue, int fileStart) throws IOException {
		if (playlistPlayer != null) {
			Observable.create(playlistPlayer).ignoreElements();

			playlistPlayer.close();
		}

		playlistPlayer = new PlaylistPlayer(preparedPlaybackFileQueue, fileStart);

		if (emitter != null) {
			try {
				playlistPlayer.subscribe(emitter);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return this;
	}

	@Override
	public void close() throws IOException {
		if (playlistPlayer != null)
			playlistPlayer.close();
	}

	@Override
	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();
	}

	@Override
	public void resume() {
		if (playlistPlayer != null)
			playlistPlayer.resume();
	}

	@Override
	public void setVolume(float volume) {
		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		emitter = e;

		if (playlistPlayer != null)
			playlistPlayer.subscribe(e);
	}
}
