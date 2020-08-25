package com.lasthopesoftware.bluewater.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.namehillsoftware.handoff.promises.Promise

class LibraryRemoval(
	private val storedItems: IStoredItemAccess,
	private val libraryStorage: ILibraryStorage) : RemoveLibraries {

	override fun removeLibrary(library: Library): Promise<Unit> =
		storedItems.promiseStoredItems(library.libraryId)
			.eventually { s -> Promise.whenAll(s.map { i -> storedItems.disableItemSync(i) }) }
			.eventually { libraryStorage.removeLibrary(library) }
}