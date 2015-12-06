package com.lasthopesoftware.bluewater.servers.connection.helpers;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by david on 8/10/15.
 */
public class ConnectionTester {

	private static final int stdTimeoutTime = 30000;

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	public static void doTest(ConnectionProvider connectionProvider, ITwoParameterRunnable<FluentTask<Integer, Void, Boolean>, Boolean> onTestComplete) {
		doTest(connectionProvider, stdTimeoutTime, onTestComplete);
	}

	public static void doTest(final ConnectionProvider connectionProvider, final int timeout, ITwoParameterRunnable<FluentTask<Integer, Void, Boolean>, Boolean> onTestComplete) {
		final FluentTask<Integer, Void, Boolean> connectionTestTask = new FluentTask<Integer, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Integer... params) {
				try {
					final HttpURLConnection conn = connectionProvider.getConnection("Alive");

					if (conn == null) return Boolean.FALSE;

					try {
						conn.setConnectTimeout(timeout);
						final InputStream is = conn.getInputStream();
						try {
							final StandardRequest responseDao = StandardRequest.fromInputStream(is);

							return responseDao != null && responseDao.isStatus();
						} finally {
							is.close();
						}
					} catch (IOException | IllegalArgumentException e) {
						mLogger.warn(e.getMessage());
					} finally {
						conn.disconnect();
					}
				} catch (IOException e) {
					mLogger.error("Error getting connection", e);
					setException(e);
				}

				return Boolean.FALSE;
			}
		};

		if (onTestComplete != null)
			connectionTestTask.onComplete(onTestComplete);

		connectionTestTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
