package io.openems.edge.core.sum;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.EnumReadChannel;
import io.openems.edge.common.channel.internal.IntegerReadChannel;
import io.openems.edge.common.channel.internal.LongReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(SumImpl c) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Sum.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ESS_SOC:
					case ESS_ACTIVE_POWER:
					case GRID_ACTIVE_POWER:
					case GRID_MAX_ACTIVE_POWER:
					case GRID_MIN_ACTIVE_POWER:
					case PRODUCTION_ACTIVE_POWER:
					case PRODUCTION_MAX_ACTIVE_POWER:
					case PRODUCTION_AC_ACTIVE_POWER:
					case PRODUCTION_MAX_AC_ACTIVE_POWER:
					case PRODUCTION_DC_ACTUAL_POWER:
					case PRODUCTION_MAX_DC_ACTUAL_POWER:
					case CONSUMPTION_ACTIVE_POWER:
					case CONSUMPTION_MAX_ACTIVE_POWER:
					case ESS_MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, 0);
					case GRID_MODE:
						return new EnumReadChannel(c, channelId, GridMode.UNDEFINED);
					case ESS_ACTIVE_CHARGE_ENERGY:
					case ESS_ACTIVE_DISCHARGE_ENERGY:
					case GRID_BUY_ACTIVE_ENERGY:
					case GRID_SELL_ACTIVE_ENERGY:
					case PRODUCTION_ACTIVE_ENERGY:
					case PRODUCTION_AC_ACTIVE_ENERGY:
					case PRODUCTION_DC_ACTIVE_ENERGY:
					case CONSUMPTION_ACTIVE_ENERGY:
						return new LongReadChannel(c, channelId, 0L);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
