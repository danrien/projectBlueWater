package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.specs.GivenAFileThatDoesNotYetExist.AndTheInputStreamCannotBeOpened;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static StoredFileJobException storedFileJobException;
	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);

	@BeforeClass
	public static void before() {
		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);
		when(fakeConnectionProvider.promiseResponse(any())).thenReturn(new Promise<>(new IOException()));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(true);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			fakeConnectionProvider,
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			f -> true,
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(
				new StoredFileJob(new ServiceFile(1), storedFile)).blockingSubscribe();
		} catch (Throwable e) {
			if (e.getCause() instanceof StoredFileJobException)
				storedFileJobException = (StoredFileJobException)e.getCause();
		}
	}

	@Test
	public void thenAStoredFileJobExceptionIsThrown() {
		assertThat(storedFileJobException).isNotNull();
	}

	@Test
	public void thenTheInnerExceptionIsAnIoException() {
		assertThat(storedFileJobException.getCause()).isInstanceOf(IOException.class);
	}

	@Test
	public void thenTheStoredFileIsAssociatedWithTheException() {
		assertThat(storedFileJobException.getStoredFile()).isEqualTo(storedFile);
	}
}