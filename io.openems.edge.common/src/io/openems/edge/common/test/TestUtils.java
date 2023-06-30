package io.openems.edge.common.test;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtils {

	private TestUtils() {

	}

	/**
	 * Finds and returns an open port.
	 *
	 * <p>
	 * Source https://stackoverflow.com/a/26644672
	 *
	 * @return an open port
	 * @throws IOException on error
	 */
	public static int findRandomOpenPortOnAllLocalInterfaces() throws IOException {
		try (var socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		}
	}
}
