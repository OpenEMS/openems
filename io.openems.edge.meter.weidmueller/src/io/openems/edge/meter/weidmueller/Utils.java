package io.openems.edge.meter.weidmueller;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(MeterWeidmuller525 c) {
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
				}), Arrays.stream(MeterWeidmuller525.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CURRENT_L1:
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
					case CURRENT_L2:
					case CURRENT_L3:
					case CURRENT_SUM:
					case HARMONIC_THD_CURRENT_L1N:
					case HARMONIC_THD_CURRENT_L2N:
					case HARMONIC_THD_CURRENT_L3N:
					case HARMONIC_THD_VOLT_L1N:
					case HARMONIC_THD_VOLT_L2N:
					case HARMONIC_THD_VOLT_L3N:
					case MEASURED_FREQUENCY:
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
					case REACTIVE_POWER_Q1_L1N:
					case REACTIVE_POWER_Q2_L2N:
					case REACTIVE_POWER_Q3_L3N:
					case REACTIVE_POWER_SUM:
					case REAL_ENERGY_L1:
					case REAL_ENERGY_L1_CONSUMED:
					case REAL_ENERGY_L1_DELIVERED:
					case REAL_ENERGY_L1_L3:
					case REAL_ENERGY_L1_L3_CONSUMED_RATE_1:
					case REAL_ENERGY_L1_L3_DELIVERED:
					case REAL_ENERGY_L2:
					case REAL_ENERGY_L2_CONSUMED:
					case REAL_ENERGY_L2_DELIVERED:
					case REAL_ENERGY_L3:
					case REAL_ENERGY_L3_CONSUMED:
					case REAL_ENERGY_L3_DELIVERED:
					case REAL_POWER_P1_L1N:
					case REAL_POWER_P2_L2N:
					case REAL_POWER_P3_L3N:
					case REAL_POWER_SUM:
					case VOLTAGE_L1_L2:
					case VOLTAGE_L1_L3:
					case VOLTAGE_L1_N:
					case VOLTAGE_L2_L3:
					case VOLTAGE_L2_N:
					case VOLTAGE_L3_N:
					case ROTATION_FIELD:
						return new IntegerReadChannel(c, channelId);
					}
					return null;

				})).flatMap(channel -> channel);
	}
}
