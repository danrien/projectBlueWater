package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenAnInternalStoragePreference;

import com.lasthopesoftware.bluewater.client.browsing.library.access.specs.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.storage.directories.specs.FakePrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.specs.FakePublicDirectoryLookup;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePrivateDirectoryLookup fakePrivateDirectoryLookup = new FakePrivateDirectoryLookup();
		fakePrivateDirectoryLookup.addDirectory("", 1);
		fakePrivateDirectoryLookup.addDirectory("", 2);
		fakePrivateDirectoryLookup.addDirectory("", 3);
		fakePrivateDirectoryLookup.addDirectory("/storage/0/my-private-sd-card", 10);

		final FakePublicDirectoryLookup publicDrives = new FakePublicDirectoryLookup();
		publicDrives.addDirectory("fake-private-path", 12);
		publicDrives.addDirectory("/fake-private-path", 5);

		final SyncDirectoryLookup syncDirectoryLookup = new SyncDirectoryLookup(
			new FakeLibraryProvider(Collections.singleton(new Library()
				.setId(1)
				.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL))),
			publicDrives,
			fakePrivateDirectoryLookup,
			fakePrivateDirectoryLookup);

		file = new FuturePromise<>(syncDirectoryLookup.promiseSyncDirectory(new LibraryId(1))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-private-sd-card/1");
	}
}
