package io.openems.edge.ess.streetscooter;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	static Stream<Channel<?>> initializeChannels(AbstractEssStreetscooter c) {
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
					case ACTIVE_CHARGE_ENERGY: // TODO ACTIVE_CHARGE_ENERGY
					case ACTIVE_DISCHARGE_ENERGY: // TODO ACTIVE_DISCHARGE_ENERGY
						return new IntegerReadChannel(c, channelId);
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, AbstractEssStreetscooter.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.ON_GRID);
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
						return new IntegerReadChannel(c, channelId);
					case SET_ACTIVE_POWER_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AbstractEssStreetscooter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case INVERTER_MODE:
						return new IntegerReadChannel(c, channelId, InverterMode.UNDEFINED);
					case BATTERY_BMS_I_ACT:
					case BATTERY_BMS_ERR:
					case BATTERY_BMS_PWR_CHRG_MAX:
					case BATTERY_BMS_SOH:
					case BATTERY_BMS_ST_BAT:
					case BATTERY_BMS_T_MAX_PACK:
					case BATTERY_BMS_T_MIN_PACK:
					case BATTERY_BMS_U_PACK:
					case BATTERY_BMS_WRN:
					case INVERTER_ACTIVE_POWER:
					case INVERTER_DC1_FAULT_VALUE:
					case INVERTER_DC2_FAULT_VALUE:
					case INVERTER_ERROR_MESSAGE_1H:
					case INVERTER_ERROR_MESSAGE_1L:
					case INVERTER_ERROR_MESSAGE_2H:
					case INVERTER_ERROR_MESSAGE_2L:
					case INVERTER_F_AC_1:
					case INVERTER_F_AC_2:
					case INVERTER_F_AC_3:
					case INVERTER_GF1_FAULT_VALUE:
					case INVERTER_GF2_FAULT_VALUE:
					case INVERTER_GF3_FAULT_VALUE:
					case INVERTER_GFCI_FAULT_VALUE:
					case INVERTER_GV1_FAULT_VALUE:
					case INVERTER_GV2_FAULT_VALUE:
					case INVERTER_GV3_FAULT_VALUE:
					case INVERTER_P_AC:
					case INVERTER_P_AC_1:
					case INVERTER_P_AC_2:
					case INVERTER_P_AC_3:
					case INVERTER_TEMPERATURE:
					case INVERTER_TEMPERATURE_FAULT_VALUE:
					case INVERTER_V_AC_1:
					case INVERTER_V_AC_2:
					case INVERTER_V_AC_3:
					case INVERTER_V_DC_1:
					case INVERTER_V_DC_2:
					case DEBUG_INVERTER_SET_ACTIVE_POWER:
					case ICU_STATUS:
						return new IntegerReadChannel(c, channelId);
					case INVERTER_SET_ACTIVE_POWER:
						return new IntegerWriteChannel(c, channelId);
					case ICU_RUN:
					case ICU_ENABLED:
						return new BooleanWriteChannel(c, channelId);
					case BATTERY_CONNECTED:
					case BATTERY_OVERLOAD:
					case ICU_RUNSTATE:
					case INVERTER_CONNECTED:
					case DEBUG_ICU_ENABLED:
					case DEBUG_ICU_RUN:
						return new BooleanReadChannel(c, channelId);
					case SYSTEM_STATE_INFORMATION:
						return new StringWriteChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
