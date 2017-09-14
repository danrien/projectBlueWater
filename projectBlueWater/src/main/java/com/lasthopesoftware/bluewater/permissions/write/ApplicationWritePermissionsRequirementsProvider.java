package com.lasthopesoftware.bluewater.permissions.write;

import android.content.Context;
import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;

public final class ApplicationWritePermissionsRequirementsProvider implements IApplicationWritePermissionsRequirementsProvider {

	private final ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider;
	private final IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs;

	public ApplicationWritePermissionsRequirementsProvider(Context context) {
		this(LibraryStorageWritePermissionsRequirementsProvider.getInstance(), new ExternalStorageWritePermissionsArbitratorForOs(context));
	}

	public ApplicationWritePermissionsRequirementsProvider(
			ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider,
			IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs) {

		this.storageWritePermissionsRequirementsProvider = storageWritePermissionsRequirementsProvider;
		this.storageWritePermissionArbitratorForOs = storageWritePermissionArbitratorForOs;
	}

	@Override
	public boolean isWritePermissionsRequiredForLibrary(@NonNull Library library) {
		return storageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(library) && !storageWritePermissionArbitratorForOs.isWritePermissionGranted();
	}
}
