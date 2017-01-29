package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;

/**
 * Created by david on 1/17/17.
 */

public class PlaybackFileChangedScrobbleDroidProxy extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		if (!SessionConnection.isBuilt()) return;

        final int fileKey = intent.getIntExtra(PlaybackService.PlaylistEvents.PlaybackFileParameters.fileKey, -1);
        if (fileKey < 0) return;

        final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), fileKey);
        filePropertiesProvider.onComplete(fileProperties -> {
            final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
            final String name = fileProperties.get(FilePropertiesProvider.NAME);
            final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
            final long duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);
            final String trackNumberString = fileProperties.get(FilePropertiesProvider.TRACK);
            final Integer trackNumber = trackNumberString != null && !trackNumberString.isEmpty() ? Integer.valueOf(trackNumberString) : null;

            final Intent scrobbleDroidIntent = getScrobbleIntent(true);
            scrobbleDroidIntent.putExtra("artist", artist);
            scrobbleDroidIntent.putExtra("album", album);
            scrobbleDroidIntent.putExtra("track", name);
            scrobbleDroidIntent.putExtra("secs", (int) (duration / 1000));
            if (trackNumber != null)
                scrobbleDroidIntent.putExtra("tracknumber", trackNumber.intValue());
        });
    }

    private Intent getScrobbleIntent(boolean isPlaying) {
        final Intent scrobbleDroidIntent = new Intent(SCROBBLE_DROID_INTENT);
        scrobbleDroidIntent.putExtra("playing", isPlaying);

        return scrobbleDroidIntent;
    }

    private static final String SCROBBLE_DROID_INTENT = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";
}