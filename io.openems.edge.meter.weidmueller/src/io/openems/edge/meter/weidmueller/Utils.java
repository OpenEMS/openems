package io.openems.edge.meter.weidmueller;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(MeterWeidmueller525 c) {
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
						return new IntegerReadChannel(c, channelId);
					case ACTIVE_CONSUMPTION_ENERGY:
					case ACTIVE_PRODUCTION_ENERGY:
						return new LongReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AsymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case VOLTAGE_L1:
					case VOLTAGE_L2:
					case VOLTAGE_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(WeidmuellerChannelId.values()).map(channelId -> {
					switch (channelId) {
					case APPARENT_ENERGY_L1:
					case APPARENT_ENERGY_L1_L3:
					case APPARENT_ENERGY_L2:
					case APPARENT_ENERGY_L3:
					case APPARENT_POWER_S1_L1N:
					case APPARENT_POWER_S2_L2N:
					case APPARENT_POWER_S3_L3N:
					case APPARENT_POWER_SUM:
					case COSPHI_L1:
					case COSPHI_L2:
					case COSPHI_L3:
					case HARMONIC_THD_CURRENT_L1N:
					case HARMONIC_THD_CURRENT_L2N:
					case HARMONIC_THD_CURRENT_L3N:
					case HARMONIC_THD_VOLT_L1N:
					case HARMONIC_THD_VOLT_L2N:
					case HARMONIC_THD_VOLT_L3N:
					case REACTIVE_ENERGY_CAPACITIVE_L1:
					case REACTIVE_ENERGY_CAPACITIVE_L1_L3:
					case REACTIVE_ENERGY_CAPACITIVE_L2:
					case REACTIVE_ENERGY_CAPACITIVE_L3:
					case REACTIVE_ENERGY_INDUCTIVE_L1:
					case REACTIVE_ENERGY_INDUCTIVE_L1_L3:
					case REACTIVE_ENERGY_INDUCTIVE_L2:
					case REACTIVE_ENERGY_INDUCTIVE_L3:
					case REACTIVE_ENERGY_L1:
					case REACTIVE_ENERGY_L1_L3:
					case REACTIVE_ENERGY_L2:
					case REACTIVE_ENERGY_L3:
					case REAL_ENERGY_L1:
					case REAL_ENERGY_L1_CONSUMED:
					case REAL_ENERGY_L1_DELIVERED:
					case REAL_ENERGY_L1_L3:
					case REAL_ENERGY_L2:
					case REAL_ENERGY_L2_CONSUMED:
					case REAL_ENERGY_L2_DELIVERED:
					case REAL_ENERGY_L3:
					case REAL_ENERGY_L3_CONSUMED:
					case REAL_ENERGY_L3_DELIVERED:
					case VOLTAGE_L1_L2:
					case VOLTAGE_L1_L3:
					case VOLTAGE_L2_L3:
					case ROTATION_FIELD:
						return new IntegerReadChannel(c, channelId);
					}
					return null;

				})).flatMap(channel -> channel);
	}
}
