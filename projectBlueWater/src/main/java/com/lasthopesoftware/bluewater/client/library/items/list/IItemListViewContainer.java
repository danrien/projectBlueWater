package com.lasthopesoftware.bluewater.client.library.items.list;

import android.widget.ViewAnimator;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;

/**
 * Created by david on 11/2/15.
 */
public interface IItemListViewContainer {
    void updateViewAnimator(ViewAnimator viewAnimator);
    NowPlayingFloatingActionButton getNowPlayingFloatingActionButton();
}
