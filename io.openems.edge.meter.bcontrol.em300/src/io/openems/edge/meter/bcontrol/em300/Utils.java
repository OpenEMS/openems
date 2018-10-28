package io.openems.edge.meter.bcontrol.em300;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(MeterBControlEM300 c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER:
					case ACTIVE_CONSUMPTION_ENERGY:
					case ACTIVE_PRODUCTION_ENERGY:
					case CURRENT:
					case FREQUENCY:
					case MAX_ACTIVE_POWER:
					case MIN_ACTIVE_POWER:
					case REACTIVE_POWER:
					case VOLTAGE:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AsymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
					case VOLTAGE_L1:
					case VOLTAGE_L2:
					case VOLTAGE_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(MeterBControlEM300.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_NEG:
					case ACTIVE_POWER_POS:
					case ACTIVE_POWER_L1_NEG:
					case ACTIVE_POWER_L1_POS:
					case ACTIVE_POWER_L2_NEG:
					case ACTIVE_POWER_L2_POS:
					case ACTIVE_POWER_L3_NEG:
					case ACTIVE_POWER_L3_POS:

					case REACTIVE_POWER_NEG:
					case REACTIVE_POWER_POS:
					case REACTIVE_POWER_L1_NEG:
					case REACTIVE_POWER_L1_POS:
					case REACTIVE_POWER_L2_NEG:
					case REACTIVE_POWER_L2_POS:
					case REACTIVE_POWER_L3_NEG:
					case REACTIVE_POWER_L3_POS:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}
