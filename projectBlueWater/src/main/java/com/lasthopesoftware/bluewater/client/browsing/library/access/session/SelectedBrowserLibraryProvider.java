package com.lasthopesoftware.bluewater.client.browsing.library.access.session;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/12/17.
 */

public class SelectedBrowserLibraryProvider implements ISelectedBrowserLibraryProvider {

	private final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider;
	private final ILibraryProvider libraryProvider;

	public SelectedBrowserLibraryProvider(ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider, ILibraryProvider libraryProvider) {
		this.selectedLibraryIdentifierProvider = selectedLibraryIdentifierProvider;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> getBrowserLibrary() {
		return libraryProvider.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId());
	}
}