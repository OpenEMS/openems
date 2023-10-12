package io.openems.edge.common.test;

import java.io.IOException;
import java.net.ServerSocket;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

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
		var channel = component.channel(channelId);
		channel.setNextValue(value);
		channel.nextProcessImage();
	}
}
