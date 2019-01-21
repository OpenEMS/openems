package io.openems.edge.controller.evcs;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EvcsController c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
					// }), Arrays.stream(EvcsController.ChannelId.values()).map(channelId -> {
					// switch (channelId) {
					// case STATE_MACHINE:
					// return new IntegerReadChannel(c, channelId);
					// }
					// return null;
				}) //
		).flatMap(channel -> channel);
	}
}
