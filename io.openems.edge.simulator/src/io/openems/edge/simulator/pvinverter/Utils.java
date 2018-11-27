package io.openems.edge.simulator.pvinverter;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;
import io.openems.edge.simulator.pvinverter.PvInverter;

import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(PvInverter c) {
		return Stream.of(//
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
					case ACTIVE_PRODUCTION_ENERGY:
					case ACTIVE_CONSUMPTION_ENERGY:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricPvInverter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_LIMIT:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(PvInverter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SIMULATED_ACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				})
		//
		).flatMap(channel -> channel);
	}
}
