package io.openems.edge.common.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

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

	/**
	 * Calls {@link Channel#nextProcessImage()} for every Channel of the
	 * {@link OpenemsComponent}.
	 * 
	 * @param component the {@link OpenemsComponent}
	 */
	public static void activateNextProcessImage(OpenemsComponent component) {
		component.channels().forEach(channel -> {
			channel.nextProcessImage();
		});
	}

	/**
	 * Sets the value on a Component Channel and activates the Process Image.
	 * 
	 * <p>
	 * This is useful to simulate a Channel value in a Unit test, as the value
	 * becomes directly available on the Channel.
	 * 
	 * @param component the {@link OpenemsComponent}
	 * @param channelId the {@link ChannelId}
	 * @param value     the new value
	 */
	public static void withValue(OpenemsComponent component, ChannelId channelId, Object value) {
		withValue(component.channel(channelId), value);
	}

	/**
	 * Sets the value on a Channel and activates the Process Image.
	 * 
	 * <p>
	 * This is useful to simulate a Channel value in a Unit test, as the value
	 * becomes directly available on the Channel.
	 * 
	 * @param channel the {@link Channel}
	 * @param value   the new value
	 */
	public static void withValue(Channel<?> channel, Object value) {
		channel.setNextValue(value);
		channel.nextProcessImage();
	}
}
