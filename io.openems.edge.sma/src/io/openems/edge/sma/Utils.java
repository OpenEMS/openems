package io.openems.edge.sma;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(SunnyIsland6Ess c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new IntegerReadChannel(c, channelId);
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, SunnyIsland6Ess.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SunnyIsland6Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SYSTEM_STATE:
					case BATTERY_TEMPERATURE:
					case BATTERY_VOLTAGE:
					case FREQUENCY:
					case BATTERY_CURRENT:
					case OPERATING_MODE_FOR_ACTIVE_POWER:
					case OPERATING_MODE_FOR_REACTIVE_POWER:
					case ABSORBED_ENERGY:
					case AMP_HOURS_COUNTER_FOR_BATTERY_CHARGE:
					case AMP_HOURS_COUNTER_FOR_BATTERY_DISCHARGE:
					case DEVICE_CLASS:
					case DEVICE_TYPE:
					case ENERGY_CONSUMED_FROM_GRID:
					case ENERGY_FED_INTO_GRID:
					case FAULT_CORRECTION_MEASURE:
					case GRID_FEED_IN_COUNTER_READING:
					case GRID_REFERENCE_COUNTER_READING:
					case MESSAGE:
					case METER_READING_CONSUMPTION_METER:
					case NUMBER_OF_EVENT_FOR_INSTALLER:
					case NUMBER_OF_EVENT_FOR_SERVICE:
					case NUMBER_OF_EVENT_FOR_USER:
					case NUMBER_OF_GENERATORS_STARTS:
					case NUMBER_OF_GRID_CONNECTIONS:
					case POWER_OUTAGE:
					case RECOMMENDED_ACTION:
					case RELEASED_ENERGY:
					case RISE_IN_SELF_CONSUMPTION:
					case RISE_IN_SELF_CONSUMPTION_TODAY:
					case SERIAL_NUMBER:
					case SOFTWARE_PACKAGE:
					case TOTAL_YIELD:
					case WAITING_TIME_UNTIL_FEED_IN:
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case GRID_VOLTAGE_L1:
					case GRID_VOLTAGE_L2:
					case GRID_VOLTAGE_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					case BMS_OPERATING_MODE:
					case MIN_SOC_POWER_ON:
					case MIN_SOC_POWER_OFF:
					case METER_SETTING:
					case SET_CONTROL_MODE:
					case SET_REACTIVE_POWER:
					case SET_ACTIVE_POWER:
					case MAXIMUM_BATTERY_CHARGING_POWER:
					case MAXIMUM_BATTERY_DISCHARGING_POWER:
					case GRID_GUARD_CODE:
						return new IntegerWriteChannel(c, channelId);

					}
					return null;
				})).flatMap(channel -> channel);
	}
}
