package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;

import java.io.IOException;


public class PreparationException extends IOException {
	private final PositionedFile positionedFile;

	public PreparationException(PositionedFile positionedFile, Throwable cause) {
		super(cause);
		this.positionedFile = positionedFile;
	}

	public PositionedFile getPositionedFile() {
		return positionedFile;
	}
}
