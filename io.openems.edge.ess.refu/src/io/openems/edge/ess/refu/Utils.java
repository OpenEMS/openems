package io.openems.edge.ess.refu;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(RefuEss c) {
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
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.ON_GRID);
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, RefuEss.MAX_APPARENT_POWER);
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
				}), Arrays.stream(AsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(ManagedAsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER_L1:
					case DEBUG_SET_ACTIVE_POWER_L2:
					case DEBUG_SET_ACTIVE_POWER_L3:
					case DEBUG_SET_REACTIVE_POWER_L1:
					case DEBUG_SET_REACTIVE_POWER_L2:
					case DEBUG_SET_REACTIVE_POWER_L3:
						return new IntegerWriteChannel(c, channelId);
					case SET_ACTIVE_POWER_L1_EQUALS:
					case SET_ACTIVE_POWER_L2_EQUALS:
					case SET_ACTIVE_POWER_L3_EQUALS:
					case SET_REACTIVE_POWER_L1_EQUALS:
					case SET_REACTIVE_POWER_L2_EQUALS:
					case SET_REACTIVE_POWER_L3_EQUALS:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;

				}), Arrays.stream(RefuEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {

					case SET_WORK_STATE:
					case SET_ACTIVE_POWER_L1:
					case SET_ACTIVE_POWER_L2:
					case SET_ACTIVE_POWER_L3:
					case SET_REACTIVE_POWER_L1:
					case SET_REACTIVE_POWER_L2:
					case SET_REACTIVE_POWER_L3:
					case SET_ACTIVE_POWER:
					case SET_OPERATION_MODE:
					case SET_REACTIVE_POWER:
					case SET_SYSTEM_ERROR_RESET:
						return new IntegerWriteChannel(c, channelId);

					case STATE_0:
					case STATE_1:
					case STATE_2:
					case STATE_3:
					case STATE_4:
					case STATE_5:
					case STATE_6:
					case STATE_7:
					case STATE_8:
					case STATE_9:
					case STATE_10:
					case STATE_100:
					case STATE_11:
					case STATE_12:
					case STATE_13:
					case STATE_14:
					case STATE_18:
					case STATE_19:
					case STATE_20:
					case STATE_21:
					case STATE_22:
					case STATE_23:
					case STATE_24:
					case STATE_25:
					case STATE_26:
					case STATE_27:
					case STATE_28:
					case STATE_29:
					case STATE_30:
					case STATE_31:
					case STATE_32:
					case STATE_33:
					case STATE_34:
					case STATE_35:
					case STATE_36:
					case STATE_37:
					case STATE_38:
					case STATE_39:
					case STATE_40:
					case STATE_41:
					case STATE_42:
					case STATE_43:
					case STATE_44:
					case STATE_45:
					case STATE_46:
					case STATE_47:
					case STATE_48:
					case STATE_49:
					case STATE_50:
					case STATE_51:
					case STATE_52:
					case STATE_53:
					case STATE_54:
					case STATE_55:
					case STATE_56:
					case STATE_57:
					case STATE_58:
					case STATE_59:
					case STATE_60:
					case STATE_61:
					case STATE_62:
					case STATE_63:
					case STATE_64:
					case STATE_65:
					case STATE_66:
					case STATE_67:
					case STATE_68:
					case STATE_69:
					case STATE_70:
					case STATE_71:
					case STATE_72:
					case STATE_73:
					case STATE_74:
					case STATE_75:
					case STATE_76:
					case STATE_77:
					case STATE_78:
					case STATE_79:
					case STATE_80:
					case STATE_81:
					case STATE_82:
					case STATE_83:
					case STATE_84:
					case STATE_85:
					case STATE_86:
					case STATE_87:
					case STATE_88:
					case STATE_89:
					case STATE_90:
					case STATE_91:
					case STATE_92:
					case STATE_93:
					case STATE_94:
					case STATE_95:
					case STATE_96:
					case STATE_97:
					case STATE_98:
					case STATE_99:
					case STATE_15:
					case STATE_16:
					case STATE_17:
					case BATTERY_CONTROL_STATE_0:
					case BATTERY_CONTROL_STATE_1:
					case BATTERY_CONTROL_STATE_10:
					case BATTERY_CONTROL_STATE_11:
					case BATTERY_CONTROL_STATE_12:
					case BATTERY_CONTROL_STATE_13:
					case BATTERY_CONTROL_STATE_14:
					case BATTERY_CONTROL_STATE_15:
					case BATTERY_CONTROL_STATE_2:
					case BATTERY_CONTROL_STATE_3:
					case BATTERY_CONTROL_STATE_4:
					case BATTERY_CONTROL_STATE_5:
					case BATTERY_CONTROL_STATE_6:
					case BATTERY_CONTROL_STATE_7:
					case BATTERY_CONTROL_STATE_8:
					case BATTERY_CONTROL_STATE_9:
					case BATTERY_ON_GRID_STATE_0:
					case BATTERY_ON_GRID_STATE_1:
					case BATTERY_ON_GRID_STATE_10:
					case BATTERY_ON_GRID_STATE_11:
					case BATTERY_ON_GRID_STATE_12:
					case BATTERY_ON_GRID_STATE_13:
					case BATTERY_ON_GRID_STATE_14:
					case BATTERY_ON_GRID_STATE_15:
					case BATTERY_ON_GRID_STATE_2:
					case BATTERY_ON_GRID_STATE_3:
					case BATTERY_ON_GRID_STATE_4:
					case BATTERY_ON_GRID_STATE_5:
					case BATTERY_ON_GRID_STATE_6:
					case BATTERY_ON_GRID_STATE_7:
					case BATTERY_ON_GRID_STATE_8:
					case BATTERY_ON_GRID_STATE_9:
					case DCDC_STATE_0:
					case DCDC_STATE_1:
					case DCDC_STATE_2:
					case DCDC_STATE_3:
					case DCDC_STATE_7:
					case DCDC_STATE_8:
					case DCDC_STATE_9:
					case INVERTER_STATE_0:
					case INVERTER_STATE_1:
					case INVERTER_STATE_10:
					case INVERTER_STATE_11:
					case INVERTER_STATE_12:
					case INVERTER_STATE_13:
					case INVERTER_STATE_2:
					case INVERTER_STATE_3:
					case INVERTER_STATE_7:
					case INVERTER_STATE_8:
					case INVERTER_STATE_9:
						return new StateChannel(c, channelId);

					case SYSTEM_STATE:
						return new IntegerReadChannel(c, channelId, SystemState.UNDEFINED);

					case BATTERY_CURRENT:
					case BATTERY_CURRENT_PCS:
					case BATTERY_MODE:
					case BATTERY_POWER:
					case BATTERY_STATE:
					case BATTERY_VOLTAGE:
					case BATTERY_VOLTAGE_PCS:
					case CURRENT:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case DCDC_STATUS:
					case PCS_ALLOWED_CHARGE:
					case PCS_ALLOWED_DISCHARGE:
					case ALLOWED_CHARGE_CURRENT:
					case COS_PHI_3P:
					case COS_PHI_L1:
					case COS_PHI_L2:
					case COS_PHI_L3:
					case ALLOWED_DISCHARGE_CURRENT:
					case BATTERY_CHARGE_ENERGY:
					case BATTERY_DISCHARGE_ENERGY:
					case BATTERY_HIGHEST_TEMPERATURE:
					case BATTERY_HIGHEST_VOLTAGE:
					case BATTERY_LOWEST_TEMPERATURE:
					case BATTERY_LOWEST_VOLTAGE:
					case BATTERY_STOP_REQUEST:
					case DCDC_ERROR_CODE:
					case ERROR_LOG_0:
					case ERROR_LOG_1:
					case ERROR_LOG_10:
					case ERROR_LOG_11:
					case ERROR_LOG_12:
					case ERROR_LOG_13:
					case ERROR_LOG_14:
					case ERROR_LOG_15:
					case ERROR_LOG_2:
					case ERROR_LOG_3:
					case ERROR_LOG_4:
					case ERROR_LOG_5:
					case ERROR_LOG_6:
					case ERROR_LOG_7:
					case ERROR_LOG_8:
					case ERROR_LOG_9:
					case INVERTER_ERROR_CODE:
					case ERROR_HANDLER_STATE:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}
