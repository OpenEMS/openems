package io.openems.edge.simulator.ess;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.api.Ess.GridMode;
import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public class EssUtils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(OpenemsComponent c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case MAX_ACTIVE_POWER:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, GridMode.ON_GRID);
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId, 0);
					}
					return null;
				}), Arrays.stream(ManagedAsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER_L1:
					case DEBUG_SET_REACTIVE_POWER_L1:
					case DEBUG_SET_ACTIVE_POWER_L2:
					case DEBUG_SET_REACTIVE_POWER_L2:
					case DEBUG_SET_ACTIVE_POWER_L3:
					case DEBUG_SET_REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
