package io.openems.edge.simulator.io;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {
	public static Stream<? extends Channel<?>> initializeChannels(DigitalInputOutput s) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(s, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}
