package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IStoredFileUriProvider {
	Promise<Uri> promiseStoredFileUri(ServiceFile serviceFile);
}