package io.openems.edge.common.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
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

	/**
	 * Helper to test a {@link #withValue(Channel, Object)} method in a JUnit test.
	 * 
	 * @param <T>    the type of the {@link AbstractDummyOpenemsComponent}
	 * @param sut    the actual system-under-test
	 * @param setter the getChannel getter method
	 * @param getter the withChannel setter method
	 */
	public static <T> void testWithValue(T sut, BiFunction<T, Integer, T> setter, Function<T, Value<Integer>> getter) {
		var before = getter.apply(sut).get();
		if (before != null) {
			throw new IllegalArgumentException("TestUtils.testWithValue() expected [null] got [" + before + "]");
		}
		setter.apply(sut, 123);
		var after = getter.apply(sut).get().intValue();
		if (after != 123) {
			throw new IllegalArgumentException("TestUtils.testWithValue() expected [123] got [" + after + "]");
		}
	}
}
