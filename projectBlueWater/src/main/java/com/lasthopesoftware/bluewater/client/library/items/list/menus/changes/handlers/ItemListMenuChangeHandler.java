package com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers;

import android.widget.ViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;

/**
 * Created by david on 11/2/15.
 */
public class ItemListMenuChangeHandler implements IItemListMenuChangeHandler {

    private final IItemListViewContainer itemListViewContainer;
    private final NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

    public ItemListMenuChangeHandler(IItemListViewContainer itemListViewContainer) {
        this.itemListViewContainer = itemListViewContainer;
        nowPlayingFloatingActionButton = itemListViewContainer.getNowPlayingFloatingActionButton();
    }

    @Override
    public void onViewChanged(ViewAnimator viewAnimator) {
        itemListViewContainer.updateViewAnimator(viewAnimator);
    }

    @Override
    public void onAllMenusHidden() {
        nowPlayingFloatingActionButton.show();
    }

    @Override
    public void onAnyMenuShown() {
        nowPlayingFloatingActionButton.hide();
    }
}
