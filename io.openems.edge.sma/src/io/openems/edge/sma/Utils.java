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
						return new IntegerReadChannel(c, channelId);
					case MIN_SOC_POWER_ON:
					case MIN_SOC_POWER_OFF:
					case METER_SETTING:
					case SET_CONTROL_MODE:
					case SET_REACTIVE_POWER:
					case SET_ACTIVE_POWER:
					case BMS_OPERATING_MODE:
					case MAXIMUM_BATTERY_CHARGING_POWER:
					case MAXIMUM_BATTERY_DISCHARGING_POWER:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}
