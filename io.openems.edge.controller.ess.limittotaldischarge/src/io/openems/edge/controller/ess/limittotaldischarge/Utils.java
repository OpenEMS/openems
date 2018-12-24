package io.openems.edge.controller.ess.limittotaldischarge;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(LimitTotalDischargeController c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(LimitTotalDischargeController.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_MACHINE:
						return new IntegerReadChannel(c, channelId);
					case AWAITING_HYSTERESIS:
						return new BooleanReadChannel(c, channelId, false);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
