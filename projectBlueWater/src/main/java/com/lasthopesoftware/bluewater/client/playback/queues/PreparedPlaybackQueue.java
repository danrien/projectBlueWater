package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.RejectionProxy;
import com.lasthopesoftware.messenger.promises.propagation.ResolutionProxy;
import com.lasthopesoftware.messenger.promises.response.ImmediateAction;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;
import com.lasthopesoftware.messenger.promises.response.ResponseAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PreparedPlaybackQueue implements
	IPreparedPlaybackFileQueue,
	ResponseAction<IBufferingPlaybackHandler>,
	ImmediateResponse<PositionedBufferingPlaybackHandler, PositionedPlaybackFile>
{
	private static final Logger logger = LoggerFactory.getLogger(PreparedPlaybackQueue.class);

	private final ReentrantReadWriteLock queueUpdateLock = new ReentrantReadWriteLock();

	private final IPreparedPlaybackQueueConfiguration configuration;
	private final IPlaybackPreparer playbackPreparerTaskFactory;
	private final Queue<PositionedPreparingFile> bufferingMediaPlayerPromises;

	private IPositionedFileQueue positionedFileQueue;

	private PositionedPreparingFile currentPreparingPlaybackHandlerPromise;

	public PreparedPlaybackQueue(IPreparedPlaybackQueueConfiguration configuration, IPlaybackPreparer playbackPreparerTaskFactory, IPositionedFileQueue positionedFileQueue) {
		this.configuration = configuration;
		this.playbackPreparerTaskFactory = playbackPreparerTaskFactory;
		this.positionedFileQueue = positionedFileQueue;
		bufferingMediaPlayerPromises = new ArrayDeque<>(configuration.getMaxQueueSize());
	}

	public PreparedPlaybackQueue updateQueue(IPositionedFileQueue newPositionedFileQueue) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			final Queue<PositionedPreparingFile> newPositionedPreparingMediaPlayerPromises = new ArrayDeque<>(configuration.getMaxQueueSize());

			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null) {
				final PositionedFile positionedFile = newPositionedFileQueue.peek();

				if (positionedPreparingFile.positionedFile.equals(positionedFile)) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					newPositionedFileQueue.poll();
					continue;
				}

				while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null) {
					positionedPreparingFile.bufferingPlaybackHandlerPromise.cancel();
				}
			}

			while ((positionedPreparingFile = newPositionedPreparingMediaPlayerPromises.poll()) != null)
				bufferingMediaPlayerPromises.offer(positionedPreparingFile);

			positionedFileQueue = newPositionedFileQueue;

			beginQueueingPreparingPlayers();

			return this;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public Promise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt) {
		currentPreparingPlaybackHandlerPromise = bufferingMediaPlayerPromises.poll();
		if (currentPreparingPlaybackHandlerPromise == null) {
			currentPreparingPlaybackHandlerPromise = getNextPreparingMediaPlayerPromise(preparedAt);
			return currentPreparingPlaybackHandlerPromise != null
				? currentPreparingPlaybackHandlerPromise.promisePositionedBufferingPlaybackHandler().then(this)
				: null;
		}

		final Promise<PositionedBufferingPlaybackHandler> positionedBufferingPlaybackHandlerPromise = new Promise<>(messenger -> {
			final Promise<PositionedBufferingPlaybackHandler> positionedBufferingPlaybackHandlerPromiseRace = Promise.whenAny(
				currentPreparingPlaybackHandlerPromise.promisePositionedBufferingPlaybackHandler(),
				new Promise<>(PositionedBufferingPlaybackHandler.emptyHandler(currentPreparingPlaybackHandlerPromise.positionedFile)));

			final Promise<PositionedBufferingPlaybackHandler> playbackHandlerPromise = positionedBufferingPlaybackHandlerPromiseRace
				.eventually(positionedBufferingPlaybackHandler -> {
					if (!positionedBufferingPlaybackHandler.isEmpty())
						return new Promise<>(positionedBufferingPlaybackHandler);

					final PositionedFile positionedFile = currentPreparingPlaybackHandlerPromise.positionedFile;
					logger.warn(positionedFile + " failed to prepare in time. Cancelling and preparing again.");

					currentPreparingPlaybackHandlerPromise.bufferingPlaybackHandlerPromise.cancel();
					currentPreparingPlaybackHandlerPromise = new PositionedPreparingFile(
						positionedFile,
						playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.getServiceFile(), preparedAt));

					return currentPreparingPlaybackHandlerPromise.promisePositionedBufferingPlaybackHandler();
				});

			playbackHandlerPromise.then(new ResolutionProxy<>(messenger));
			playbackHandlerPromise.excuse(new RejectionProxy(messenger));
		});

		return positionedBufferingPlaybackHandlerPromise.then(this);
	}

	private PositionedPreparingFile getNextPreparingMediaPlayerPromise(int preparedAt) {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();

		final PositionedFile positionedFile;
		try {
			positionedFile = positionedFileQueue.poll();
		} finally {
			writeLock.unlock();
		}

		if (positionedFile == null) return null;

		return
			new PositionedPreparingFile(
				positionedFile,
				playbackPreparerTaskFactory.promisePreparedPlaybackHandler(positionedFile.getServiceFile(), preparedAt));
	}

	@Override
	public PositionedPlaybackFile respond(PositionedBufferingPlaybackHandler positionedBufferingPlaybackHandler) {
		positionedBufferingPlaybackHandler.bufferingPlaybackHandler.bufferPlaybackFile().then(ImmediateAction.perform(this));

		return new PositionedPlaybackFile(positionedBufferingPlaybackHandler.bufferingPlaybackHandler, positionedBufferingPlaybackHandler.positionedFile);
	}

	@Override
	public void perform(IBufferingPlaybackHandler bufferingPlaybackHandler) {
		beginQueueingPreparingPlayers();
	}

	private void beginQueueingPreparingPlayers() {
		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			if (bufferingMediaPlayerPromises.size() >= configuration.getMaxQueueSize()) return;

			final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
			if (nextPreparingMediaPlayerPromise == null) return;

			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise);

			nextPreparingMediaPlayerPromise.promisePositionedBufferingPlaybackHandler().then(this);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() throws IOException {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.bufferingPlaybackHandlerPromise.cancel();

		final ReentrantReadWriteLock.WriteLock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null)
				positionedPreparingFile.bufferingPlaybackHandlerPromise.cancel();
		} finally {
			writeLock.unlock();
		}
	}

	private static class PositionedPreparingFile {
		final PositionedFile positionedFile;
		final Promise<IBufferingPlaybackHandler> bufferingPlaybackHandlerPromise;

		private PositionedPreparingFile(PositionedFile positionedFile, Promise<IBufferingPlaybackHandler> bufferingPlaybackHandlerPromise) {
			this.positionedFile = positionedFile;
			this.bufferingPlaybackHandlerPromise = bufferingPlaybackHandlerPromise;
		}

		Promise<PositionedBufferingPlaybackHandler> promisePositionedBufferingPlaybackHandler() {
			return
				bufferingPlaybackHandlerPromise
					.then(handler -> new PositionedBufferingPlaybackHandler(positionedFile, handler));
		}
	}
}
