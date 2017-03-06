package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 3/5/17.
 */

public class MediaPlayerPlaybackPreparerProvider implements IPlaybackPreparerProvider {

	private final IFileUriProvider fileUriProvider;
	private final Context context;
	private final Library library;

	public MediaPlayerPlaybackPreparerProvider(Context context, IFileUriProvider fileUriProvider, Library library) {
		this.fileUriProvider = fileUriProvider;
		this.context = context;
		this.library = library;
	}

	@Override
	public IPlaybackPreparer providePlaybackPreparer() {
		return new MediaPlayerPlaybackPreparer(
			fileUriProvider,
			new MediaPlayerInitializer(context, library));
	}
}