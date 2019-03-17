package io.openems.edge.kostal.piko.core.impl;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.kostal.piko.core.api.BatteryCurrentDirection;
import io.openems.edge.kostal.piko.core.api.KostalPikoCore;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KostalPikoCoreImpl c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(KostalPikoCore.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case UNABLE_TO_READ_DATA:
						return new StateChannel(c, channelId);

					case INVERTER_NAME:
					case ARTICLE_NUMBER:
					case INVERTER_SERIAL_NUMBER:
					case FIRMWARE_VERSION:
					case HARDWARE_VERSION:
					case KOMBOARD_VERSION:
					case PARAMETER_VERSION:
					case COUNTRY_NAME:
					case INVERTER_OPERATING_STATUS:
					case INVERTER_TYPE_NAME:
						return new StringReadChannel(c, channelId);

					case NUMBER_OF_STRING:
					case NUMBER_OF_PHASES:
					case POWER_ID:
					case PRESENT_ERROR_EVENT_CODE_1:
					case PRESENT_ERROR_EVENT_CODE_2:
					case FEED_IN_TIME:
					case INVERTER_STATUS:
					case ADDRESS_MODBUS_RTU:
					case BAUDRATE_INDEX_MODBUS_RTU:
					case SETTING_MANUAL_IP1:
					case SETTING_MANUAL_IP2:
					case SETTING_MANUAL_IP3:
					case SETTING_MANUAL_IP4:
					case SETTING_MANUAL_SUBNET_MASK_1:
					case SETTING_MANUAL_SUBNET_MASK_2:
					case SETTING_MANUAL_SUBNET_MASK_3:
					case SETTING_MANUAL_SUBNET_MASK_4:
					case SETTING_MANUAL_GATEWAY_1:
					case SETTING_MANUAL_GATEWAY_2:
					case SETTING_MANUAL_GATEWAY_3:
					case SETTING_MANUAL_GATEWAY_4:
					case SETTING_MANUAL_IP_DNS_FIRST_1:
					case SETTING_MANUAL_IP_DNS_FIRST_2:
					case SETTING_MANUAL_IP_DNS_FIRST_3:
					case SETTING_MANUAL_IP_DNS_FIRST_4:
					case SETTING_MANUAL_IP_DNS_SECOND_1:
					case SETTING_MANUAL_IP_DNS_SECOND_2:
					case SETTING_MANUAL_IP_DNS_SECOND_3:
					case SETTING_MANUAL_IP_DNS_SECOND_4:
						return new IntegerReadChannel(c, channelId);

					case FEED_IN_STATUS:
					case SETTING_AUTO_IP:
					case SETTING_MANUAL_EXTERNAL_ROUTER:
					case PRELOAD_MODBUS_RTU:
					case TERMINATION_MODBUS_RTU:
						return new BooleanReadChannel(c, channelId);

					case OVERALL_DC_CURRENT:
					case OVERALL_DC_POWER:
					case DC_CURRENT_STRING_1:
					case DC_VOLTAGE_STRING_1:
					case DC_POWER_STRING_1:
					case DC_CURRENT_STRING_2:
					case DC_VOLTAGE_STRING_2:
					case DC_POWER_STRING_2:
					case DC_CURRENT_STRING_3:
					case DC_VOLTAGE_STRING_3:
					case DC_POWER_STRING_3:
					case BATTERY_CURRENT:
					case BATTERY_VOLTAGE:
					case BATTERY_TEMPERATURE:
					case AC_CURRENT_L1:
					case AC_VOLTAGE_L1:
					case AC_POWER_L1:
					case AC_CURRENT_L2:
					case AC_VOLTAGE_L2:
					case AC_POWER_L2:
					case AC_CURRENT_L3:
					case AC_VOLTAGE_L3:
					case AC_POWER_L3:
					case POWER_LIMITATION_OF_EVU:
					case GRID_FREQUENCY:
					case COSINUS_PHI:
					case HOME_CONSUMPTION_PV:
					case HOME_CONSUMPTION_BAT:
					case HOME_CONSUMPTION_GRID:
					case HOME_CURRENT_FROM_EXT_SENSOR_L1:
					case HOME_POWER_L1:
					case HOME_CONSUMPTION_L1:
					case HOME_CURRENT_FROM_EXT_SENSOR_L2:
					case HOME_POWER_L2:
					case HOME_CONSUMPTION_L2:
					case HOME_CURRENT_FROM_EXT_SENSOR_L3:
					case HOME_POWER_L3:
					case HOME_CONSUMPTION_L3:
					case HOME_TOTAL_POWER:
					case HOME_SELF_CONSUMPTION_TOTAL:
					case ISOLATION_RESISTOR:
					case MAX_RESIDUAL_CURRENT:
					case ANALOG_INPUT_CH_1:
					case ANALOG_INPUT_CH_2:
					case ANALOG_INPUT_CH_3:
					case ANALOG_INPUT_CH_4:
					case YIELD_TOTAL:
					case YIELD_DAY:
					case HOME_CONSUMPTION_TOTAL:
					case HOME_CONSUMPTION_DAY:
					case SELF_CONSUMPTION_TOTAL:
					case SELF_CONSUMPTION_DAY:
					case SELF_CONSUMPTION_RATE_TOTAL:
					case SELF_CONSUMPTION_RATE_DAY:
					case DEGREE_OF_SELF_SUFFICIENCY_DAY:
					case DEGREE_OF_SELF_SUFFICIENCY_TOTAL:
					case GRID_AC_P_TOTAL:
					case ACTUAL_POWER:
						return new FloatReadChannel(c, channelId);
					case BATTERY_CURRENT_DIRECTION:
						return new EnumReadChannel(c, channelId, BatteryCurrentDirection.UNDEFINED);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
