package io.openems.edge.fenecon.dess.ess;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(FeneconDessEss c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case GRID_MODE:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case SOC:
						return new IntegerReadChannel(c, channelId);
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, 9000);
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new LongReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(FeneconDessEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SYSTEM_STATE:
					case BSMU_WORK_STATE:
					case STACK_CHARGE_STATE:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
