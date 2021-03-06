package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnExternalStoragePreference;

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup;
import com.lasthopesoftware.storage.directories.FakePublicDirectoryLookup;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheSyncDrive {

	private static File file;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final FakePublicDirectoryLookup publicDrives = new FakePublicDirectoryLookup();
		publicDrives.addDirectory("", 1);
		publicDrives.addDirectory("", 2);
		publicDrives.addDirectory("", 3);
		publicDrives.addDirectory("/storage/0/my-big-sd-card", 4);

		final FakePrivateDirectoryLookup fakePrivateDirectoryLookup = new FakePrivateDirectoryLookup();
		fakePrivateDirectoryLookup.addDirectory("fake-private-path", 3);
		fakePrivateDirectoryLookup.addDirectory("/fake-private-path", 5);

		final SyncDirectoryLookup syncDirectoryLookup = new SyncDirectoryLookup(
			new FakeLibraryProvider(new Library().setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL).setId(14)),
			publicDrives,
			fakePrivateDirectoryLookup,
			publicDrives);

		file = new FuturePromise<>(syncDirectoryLookup.promiseSyncDirectory(new LibraryId(14))).get();
	}

	@Test
	public void thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file.getPath()).isEqualTo("/storage/0/my-big-sd-card/14");
	}
}
