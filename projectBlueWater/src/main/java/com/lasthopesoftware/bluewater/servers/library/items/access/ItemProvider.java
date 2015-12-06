package com.lasthopesoftware.bluewater.servers.library.items.access;

import android.util.LruCache;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.LibraryViewsProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.providers.AbstractCollectionProvider;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ItemProvider extends AbstractCollectionProvider<Item> {

    private static class ItemHolder {
        public ItemHolder(Integer revision, List<Item> items) {
            this.revision = revision;
            this.items = items;
        }

        public final Integer revision;
        public final List<Item> items;
    }

    private static final int maxSize = 50;
    private static final LruCache<Integer, ItemHolder> itemsCache = new LruCache<>(maxSize);

    private final int itemKey;

	private final ConnectionProvider connectionProvider;

	public static ItemProvider provide(ConnectionProvider connectionProvider, int itemKey) {
		return new ItemProvider(connectionProvider, itemKey);
	}
	
	public ItemProvider(ConnectionProvider connectionProvider, int itemKey) {
		super(connectionProvider, LibraryViewsProvider.browseLibraryParameter, "ID=" + String.valueOf(itemKey), "Version=2");

		this.connectionProvider = connectionProvider;
        this.itemKey = itemKey;
	}

    @Override
    protected List<Item> getData(HttpURLConnection connection) throws Exception {
        final Integer serverRevision = RevisionChecker.getRevision(connectionProvider);
        final Integer boxedItemKey = itemKey;

        ItemHolder itemHolder;
        synchronized (itemsCache) {
            itemHolder = itemsCache.get(boxedItemKey);
        }

        if (itemHolder != null && itemHolder.revision.equals(serverRevision))
            return itemHolder.items;

        if (isCancelled()) return new ArrayList<>();

        final InputStream is = connection.getInputStream();
        try {
            final List<Item> items = ItemResponse.GetItems(connectionProvider, is);

            itemHolder = new ItemHolder(serverRevision, items);

            synchronized (itemsCache) {
                itemsCache.put(boxedItemKey, itemHolder);
            }

            return items;
        } finally {
            is.close();
        }
	}
}
