package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Queue;

public class UrlScanner implements BuildUrlProviders {

	private final TestConnections connectionTester;
	private final LookupServers serverLookup;

	public UrlScanner(TestConnections connectionTester, LookupServers serverLookup) {
		this.connectionTester = connectionTester;
		this.serverLookup = serverLookup;
	}

	@Override
	public Promise<IUrlProvider> promiseBuiltUrlProvider(Library library) {
		if (library == null)
			return new Promise<>(new IllegalArgumentException("The library cannot be null"));

		if (library.getAccessCode() == null)
			return new Promise<>(new IllegalArgumentException("The access code cannot be null"));

		final String authKey = library.getAuthKey();
		final MediaServerUrlProvider mediaServerUrlProvider;
		try {
			mediaServerUrlProvider = new MediaServerUrlProvider(
				authKey,
				parseAccessCode(library.getAccessCode()));
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}

		return connectionTester.promiseIsConnectionPossible(new ConnectionProvider(mediaServerUrlProvider))
			.eventually(isValid -> isValid
				? new Promise<>(mediaServerUrlProvider)
				: serverLookup.promiseServerInformation(library)
				.eventually(info -> {
					final SchemePortPair[] availableSchemes = info.getHttpsPort() == null
						? new SchemePortPair[] { new SchemePortPair("http", info.getHttpPort()) }
						: new SchemePortPair[] { new SchemePortPair("https", info.getHttpsPort()), new SchemePortPair("http", info.getHttpPort()) };

					final Queue<IUrlProvider> mediaServerUrlProvidersQueue = new ArrayDeque<>();
					for (final SchemePortPair schemePortPair : availableSchemes) {
						mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
							authKey,
							schemePortPair.scheme,
							info.getRemoteIp(),
							schemePortPair.port));

						for (String ip : info.getLocalIps()) {
							mediaServerUrlProvidersQueue.offer(new MediaServerUrlProvider(
								authKey,
								schemePortPair.scheme,
								ip,
								schemePortPair.port));
						}
					}

					return testUrls(mediaServerUrlProvidersQueue);
				}));
	}

	private Promise<IUrlProvider> testUrls(Queue<IUrlProvider> urls) {
		final IUrlProvider urlProvider = urls.poll();
		if (urlProvider == null) return Promise.empty();

		return connectionTester
			.promiseIsConnectionPossible(new ConnectionProvider(urlProvider))
			.eventually(result -> result ? new Promise<>(urlProvider) : testUrls(urls));
	}

	private static URL parseAccessCode(String accessCode) throws MalformedURLException {
		final String[] urlParts = accessCode.split(":", 2);
		final int port =
			urlParts.length > 1 && isPositiveInteger(urlParts[1])
				? Integer.parseInt(urlParts[1])
				: 80;
		return new URL("http", urlParts[0], port, "");
	}

	private static boolean isPositiveInteger(String string) {
		for (final char c : string.toCharArray())
			if (!Character.isDigit(c)) return false;

		return true;
	}

	private static class SchemePortPair {
		final String scheme;
		final int port;

		SchemePortPair(String scheme, int port) {
			this.scheme = scheme;
			this.port = port;
		}
	}
}