package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;

public final class PlayClickHandler<TItem extends IItem> extends AbstractMenuClickHandler {
	private final IFileListParameterProvider<TItem> fileListParameterProvider;
	private final TItem item;

    public PlayClickHandler(NotifyOnFlipViewAnimator menuContainer, IFileListParameterProvider<TItem> fileListParameterProvider, TItem item) {
        super(menuContainer);
		this.fileListParameterProvider = fileListParameterProvider;

		this.item = item;
    }

    @Override
    public void onClick(final View v) {
    	SessionConnection.getInstance(v.getContext()).promiseSessionConnection()
			.then(FileStringListProvider::new)
			.eventually(p -> p.promiseFileStringList(FileListParameters.Options.None, fileListParameterProvider.getFileListParameters(item)))
			.then(new OnGetFileStringListForClickCompleteListener(v.getContext()))
			.excuse(new OnGetFileStringListForClickErrorListener(v, this))
			.excuse(new UnexpectedExceptionToasterResponse(v.getContext()));

        super.onClick(v);
    }
}