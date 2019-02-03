package io.openems.edge.simulator.evcs;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(SimulatedEvcs c) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Evcs.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CHARGE_POWER:
						return new IntegerReadChannel(c, channelId);
					case SET_CHARGE_POWER:
						return new IntegerWriteChannel(c, channelId);
					case SET_DISPLAY_TEXT:
						return new StringWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SimulatedEvcs.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SIMULATED_CHARGE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				})
		//
		).flatMap(channel -> channel);
	}
}
