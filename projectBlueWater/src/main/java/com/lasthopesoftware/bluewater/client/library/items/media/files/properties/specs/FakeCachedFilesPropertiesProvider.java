package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.HashMap;
import java.util.Map;

public class FakeCachedFilesPropertiesProvider extends CachedFilePropertiesProvider {
	private final Map<ServiceFile, Map<String, String>> cachedFileProperties = new HashMap<>();

	public FakeCachedFilesPropertiesProvider() {
		super(null, null, null);
	}

	@Override
	public Promise<Map<String, String>> promiseFileProperties(ServiceFile serviceFile) {
		try {
			return new Promise<>(cachedFileProperties.get(serviceFile));
		} catch (Throwable e) {
			return new Promise<>(e);
		}
	}

	public void addFilePropertiesToCache(ServiceFile serviceFile, Map<String, String> fileProperties) {
		cachedFileProperties.put(serviceFile, fileProperties);
	}
}
