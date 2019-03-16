package io.openems.edge.ess.core.power;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.BooleanReadChannel;
import io.openems.edge.common.channel.internal.EnumReadChannel;
import io.openems.edge.common.channel.internal.IntegerReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.power.api.SolverStrategy;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(PowerComponent c) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(PowerComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOLVED:
						return new BooleanReadChannel(c, channelId);
					case SOLVE_DURATION:
						return new IntegerReadChannel(c, channelId);
					case SOLVE_STRATEGY:
						return new EnumReadChannel(c, channelId, SolverStrategy.UNDEFINED);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
