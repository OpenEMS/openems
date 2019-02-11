package io.openems.edge.controller.ess.onefullcycle;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssOneFullCycle c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(Controller.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case RUN_FAILED:
						return new StateChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(EssOneFullCycle.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_MACHINE:
					case CYCLE_ORDER:
						return new IntegerReadChannel(c, channelId);
					case AWAITING_HYSTERESIS:
						return new BooleanReadChannel(c, channelId, false);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
