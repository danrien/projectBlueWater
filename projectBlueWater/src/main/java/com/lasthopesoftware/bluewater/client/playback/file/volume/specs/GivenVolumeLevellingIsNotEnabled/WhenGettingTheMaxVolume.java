package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenVolumeLevellingIsNotEnabled;


import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.IFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenGettingTheMaxVolume {

	private static float returnedVolume;

	@BeforeClass
	public static void before() throws InterruptedException {
		final CachedFilePropertiesProvider cachedFilePropertiesProvider =
			new CachedFilePropertiesProvider(
				mock(IConnectionProvider.class),
				mock(IFilePropertiesContainerRepository.class),
				mock(IFilePropertiesProvider.class));

		final IVolumeLevelSettings volumeLevelSettings = mock(IVolumeLevelSettings.class);

		final MaxFileVolumeProvider maxFileVolumeProvider =
			new MaxFileVolumeProvider(volumeLevelSettings, cachedFilePropertiesProvider);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		maxFileVolumeProvider
			.promiseMaxFileVolume(new ServiceFile(1))
			.then(volume -> {
				returnedVolume = volume;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenTheReturnedVolumeIsOne() {
		assertThat(returnedVolume).isEqualTo(1);
	}
}
