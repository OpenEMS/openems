package io.openems.edge.pvinverter.solarlog;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(SolarLog c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case FREQUENCY:
					case ACTIVE_POWER:
					case MAX_ACTIVE_POWER:
					case MIN_ACTIVE_POWER:
					case REACTIVE_POWER:
					case CURRENT:
					case VOLTAGE:
					case ACTIVE_CONSUMPTION_ENERGY:
					case ACTIVE_PRODUCTION_ENERGY:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricPvInverter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_LIMIT:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SolarLog.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case LAST_UPDATE_TIME:
					case MONTHLY_YIELD:
					case MONTHLY_YIELD_CONS:
					case PAC_CONSUMPTION:
					case PDC:
					case TOTAL_YIELD:
					case TOTAL_YIELD_CONS:
					case UDC:
					case YEARLY_YIELD:
					case YEARLY_YIELD_CONS:
					case YESTERDAY_YIELD:
					case YESTERDAY_YIELD_CONS:
					case TOTAL_POWER:
					case P_LIMIT_PERC_N:
					case STATUS:
						return new IntegerReadChannel(c, channelId);
					case P_LIMIT_PERC:
					case P_LIMIT_TYPE:
					case WATCH_DOG_TAG:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}