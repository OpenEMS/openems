package io.openems.edge.ess.streetscooter;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.api.SymmetricEss;
import io.openems.edge.ess.symmetric.readonly.api.SymmetricEssReadonly;

public class Utils {
	static Stream<Channel<?>> initializeChannels(AbstractEssStreetscooter c) {		
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
						return new IntegerReadChannel(c, channelId);
					case MAX_ACTIVE_POWER:
						return new IntegerReadChannel(c, channelId, AbstractEssStreetscooter.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, Ess.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(SymmetricEssReadonly.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER:
					case CHARGE_ACTIVE_POWER:
					case DISCHARGE_ACTIVE_POWER:
					case REACTIVE_POWER:
					case CHARGE_REACTIVE_POWER:
					case DISCHARGE_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AbstractEssStreetscooter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case INVERTER_MODE:
					case BATTERY_BMS_I_ACT:
					case BATTERY_BMS_ERR:
					case BATTERY_BMS_PWR_CHRG_MAX:
					case BATTERY_BMS_PWR_D_CHA_MAX:
					case BATTERY_BMS_PWR_RGN_MAX:
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
						return new IntegerReadChannel(c, channelId);
					case SET_ACTIVE_POWER:
						return new IntegerWriteChannel(c, channelId);
					case ICU_RUN:
					case ICU_ENABLED:
						return new BooleanWriteChannel(c, channelId);
					}
					return null;
				})  // 
		).flatMap(channel -> channel);
	}
	
}
