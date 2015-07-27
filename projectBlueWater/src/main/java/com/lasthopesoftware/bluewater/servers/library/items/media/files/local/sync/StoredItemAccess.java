package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.store.StoredItem;
import com.lasthopesoftware.bluewater.servers.store.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 7/5/15.
 */
public class StoredItemAccess {

    private static final Logger mLogger = LoggerFactory.getLogger(StoredItemAccess.class);

    private final Context mContext;

    public StoredItemAccess(Context context) {
        mContext = context;
    }

    public void toggleSync(IItem item, boolean enable) {
	    if (enable)
            enableItemSync(item, getListType(item));
	    else
		    disableItemSync(item, getListType(item));
    }

    public void isItemMarkedForSync(final IItem item, ISimpleTask.OnCompleteListener<Void, Void, Boolean> isItemSyncedResult) {
        final SimpleTask<Void, Void, Boolean> isItemSyncedTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, Boolean>() {
            @Override
            public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
                final Dao<StoredItem, Integer> storedListAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredItem.class);
                return isItemMarkedForSync(storedListAccess, LibrarySession.GetActiveLibrary(mContext), item, getListType(item));
            }
        });

        if (isItemSyncedResult != null)
            isItemSyncedTask.addOnCompleteListener(isItemSyncedResult);

        isItemSyncedTask.execute(DatabaseHandler.databaseExecutor);
    }

    private void enableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        DatabaseHandler.databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Dao<StoredItem, Integer> storedListAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredItem.class);

                    final Library library = LibrarySession.GetActiveLibrary(mContext);
                    if (isItemMarkedForSync(storedListAccess, library, item, itemType)) return;

                    final StoredItem storedItem = new StoredItem();
                    storedItem.setLibrary(library);
                    storedItem.setServiceId(item.getKey());
                    storedItem.setItemType(itemType);

                    try {
                        storedListAccess.create(storedItem);
                    } catch (SQLException e) {
                        mLogger.error("Error while creating new stored list", e);
                    }
                } catch (SQLException e) {
                    mLogger.error("Error getting access to the stored list table", e);
                }
            }
        });
    }

    private void disableItemSync(final IItem item, final StoredItem.ItemType itemType) {
        DatabaseHandler.databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Dao<StoredItem, Integer> storedListAccess = DatabaseHandler.getInstance(mContext).getAccessObject(StoredItem.class);

                    final StoredItem storedItem = getStoredList(storedListAccess, LibrarySession.GetActiveLibrary(mContext), item, itemType);
	                if (storedItem == null) return;

	                try {
		                storedListAccess.delete(storedItem);
                    } catch (SQLException e) {
                        mLogger.error("Error removing stored list", e);
                    }
                } catch (SQLException e) {
                    mLogger.error("Error getting access to the stored list table", e);
                }
            }
        });
    }

	public void getAllStoredItems(ISimpleTask.OnCompleteListener<Void, Void, List<StoredItem>> onStoredListsRetrieved) {
		final SimpleTask<Void, Void, List<StoredItem>> getAllStoredListsTasks = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, List<StoredItem>>() {
			@Override
			public List<StoredItem> onExecute(ISimpleTask<Void, Void, List<StoredItem>> owner, Void... params) throws Exception {
				try {
					final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
					final Dao<StoredItem, Integer> storedItemAccess = dbHandler.getAccessObject(StoredItem.class);
					return storedItemAccess.queryForAll();
				} catch (SQLException e) {
					mLogger.error("Error accessing the stored list access", e);
				}

				return new ArrayList<>();
			}
		});

		if (onStoredListsRetrieved != null)
			getAllStoredListsTasks.addOnCompleteListener(onStoredListsRetrieved);

		getAllStoredListsTasks.execute(DatabaseHandler.databaseExecutor);
	}

    private static boolean isItemMarkedForSync(Dao<StoredItem, Integer> storedListAccess, Library library, IItem item, StoredItem.ItemType itemType) {
        return getStoredList(storedListAccess, library, item, itemType) != null;
    }

    private  static StoredItem getStoredList(Dao<StoredItem, Integer> storedListAccess, Library library, IItem item, StoredItem.ItemType itemType) {
        try {
            final PreparedQuery<StoredItem> storedListPreparedQuery =
                    storedListAccess
                            .queryBuilder()
                            .where()
                            .eq(StoredItem.serviceIdColumnName, item.getKey())
                            .and()
                            .eq(StoredItem.libraryIdColumnName, library.getId())
                            .and()
                            .eq(StoredItem.itemTypeColumnName, itemType)
                            .prepare();
            return storedListAccess.queryForFirst(storedListPreparedQuery);
        } catch (SQLException e) {
            mLogger.error("Error while checking whether stored list exists.", e);
        }

        return null;
    }

	private static StoredItem.ItemType getListType(IItem item) {
		return item instanceof Playlist ? StoredItem.ItemType.PLAYLIST : StoredItem.ItemType.ITEM;
	}
}
