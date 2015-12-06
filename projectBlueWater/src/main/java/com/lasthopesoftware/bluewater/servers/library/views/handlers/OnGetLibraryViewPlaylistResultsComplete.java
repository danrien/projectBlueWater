package com.lasthopesoftware.bluewater.servers.library.views.handlers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.threading.FluentTask;

import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public class OnGetLibraryViewPlaylistResultsComplete extends OnGetLibraryViewIItemResultsComplete<Playlist> {

    private final ListView listView;
    private final Activity activity;

    public OnGetLibraryViewPlaylistResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler) {
        super(activity, container, listView, loadingView, position, itemListMenuChangeHandler);

        this.listView = listView;
        this.activity = activity;
    }

    public void run(FluentTask<String, Void, List<Playlist>> owner, List<Playlist> result) {
        super.run(owner, result);
        if (result == null) return;

        listView.setOnItemClickListener(new ClickPlaylistListener(activity, result));
    }
}
