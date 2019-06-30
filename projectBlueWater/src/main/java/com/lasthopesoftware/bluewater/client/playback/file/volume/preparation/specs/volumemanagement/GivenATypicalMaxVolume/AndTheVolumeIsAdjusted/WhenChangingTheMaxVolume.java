package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenATypicalMaxVolume.AndTheVolumeIsAdjusted;


import com.lasthopesoftware.bluewater.client.playback.file.EmptyFileVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenChangingTheMaxVolume {
	private static final EmptyFileVolumeManager playbackHandler = new EmptyFileVolumeManager();

	@BeforeClass
	public static void before() {
		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(playbackHandler, .58f);
		maxFileVolumeManager.setMaxFileVolume(.8f);
		maxFileVolumeManager.setVolume(.23f);
		maxFileVolumeManager.setMaxFileVolume(.47f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(playbackHandler.getVolume()).isCloseTo(.1081f, offset(.001f));
	}
}
