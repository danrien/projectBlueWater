package com.lasthopesoftware.bluewater.client.connection;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class ConnectionLostFilter {
	public static boolean isConnectionLostException(Throwable error) {
		return error instanceof IOException && isConnectionLostException((IOException)error);
	}

	private static boolean isConnectionLostException(IOException ioException) {
		return ioException instanceof SocketTimeoutException
			|| ioException instanceof EOFException
			|| ioException instanceof ConnectException;
	}
}
