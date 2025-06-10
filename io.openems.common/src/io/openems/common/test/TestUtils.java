package io.openems.common.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;

public class TestUtils {

	private TestUtils() {
	}

	/**
	 * Creates a {@link TimeLeapClock} for 1st January 2000 00:00.
	 * 
	 * @return the {@link TimeLeapClock}
	 */
	public static TimeLeapClock createDummyClock() {
		return new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */);
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
