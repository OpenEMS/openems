package io.openems.edge.ess.sinexcel;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssSinexcel ess) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(ess, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case ACTIVE_CHARGE_ENERGY: // TODO ACTIVE_CHARGE_ENERGY
					case ACTIVE_DISCHARGE_ENERGY: // TODO ACTIVE_DISCHARGE_ENERGY
					case GRID_MODE:
						return new IntegerReadChannel(ess, channelId);

					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(ess, channelId, 30000);
					
						
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(ess, channelId);
					
					case ALLOWED_CHARGE_POWER:
						return new IntegerReadChannel(ess, channelId, -30000);

					case ALLOWED_DISCHARGE_POWER:	
						return new IntegerReadChannel(ess, channelId, 30000);

					case SET_ACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_GREATER_OR_EQUALS:
					case SET_REACTIVE_POWER_GREATER_OR_EQUALS:
					case SET_REACTIVE_POWER_LESS_OR_EQUALS:
						return new IntegerWriteChannel(ess, channelId);
					}
					return null;
				}), Arrays.stream(EssSinexcel.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					
//----------------------------------------------------------------------------------------------------------------------
					
					
					case Analog_GridCurrent_Freq:
					case Analog_ActivePower_Rms_Value_L1:
					case Analog_ActivePower_Rms_Value_L2:
					case Analog_ActivePower_Rms_Value_L3:
					case Analog_ReactivePower_Rms_Value_L1:
					case Analog_ReactivePower_Rms_Value_L2:
					case Analog_ReactivePower_Rms_Value_L3:
					case Analog_ApparentPower_L1:
					case Analog_ApparentPower_L2:
					case Analog_ApparentPower_L3:
					case Analog_PF_RMS_Value_L1:
					case Analog_PF_RMS_Value_L2:
					case Analog_PF_RMS_Value_L3:
					case Analog_ActivePower_3Phase:
					case Analog_ReactivePower_3Phase:
					case Analog_ApparentPower_3Phase:
					case Analog_PowerFactor_3Phase:
					case Analog_CHARGE_Energy:
					case Analog_DISCHARGE_Energy:
					case Analog_REACTIVE_Energy:
					case Analog_Reactive_Energy_2:
					case Target_OffGrid_Voltage:
					case Target_OffGrid_Frequency:
					case Analog_DC_CHARGE_Energy:
					case Analog_DC_DISCHARGE_Energy:
					case ANTI_ISLANDING:	
					case MOD_ON_CMD:
					case MOD_OFF_CMD:
					case GRID_ON_CMD:
					case GRID_OFF_CMD:
					case SUNSPEC_DID_0103:
					case DC_Voltage:
					case AC_Power:
					case AC_Apparent_Power:
					case AC_Reactive_Power:	
					case Frequency:	
					case Temperature:
					case InvOutVolt_L1:
					case InvOutVolt_L2:
					case InvOutVolt_L3:
					case InvOutCurrent_L1:
					case InvOutCurrent_L2:
					case InvOutCurrent_L3:
					case DC_Current:
					case DC_Power:
					case Sinexcel_State:
					case EVENT_1:
					case Test_Register:
					case Max_Discharge_Current:
					case Max_Charge_Current:
					case Lower_Voltage_Limit:
					case Upper_Voltage_Limit:
					case Target_Active_Power:
					case Target_Reactive_Power:
					case DEBUG_EN_LIMIT:
					case BAT_MIN_CELL_VOLTAGE:
					case BAT_VOLTAGE:
					case DEBUG_DIS_MAX_A:
					case DEBUG_CHA_MAX_A:
					case DEBUG_CHA_MAX_V:
					case DEBUG_DIS_MIN_V:
		
						return new IntegerReadChannel(ess, channelId);
					case SETDATA_MOD_ON_CMD:
					case SETDATA_MOD_OFF_CMD:
					case SETDATA_GRID_ON_CMD:
					case SETDATA_GRID_OFF_CMD:
					case SET_ANTI_ISLANDING:
					case SET_CHARGE_DISCHARGE_ACTIVE:
					case SET_CHARGE_DISCHARGE_REACTIVE:
					case SET_CHARGE_CURRENT:
					case SET_DISCHARGE_CURRENT:
					case SET_SLOW_CHARGE_VOLTAGE:
					case SET_FLOAT_CHARGE_VOLTAGE:
					case SET_UPPER_VOLTAGE:
					case SET_LOWER_VOLTAGE:
					case BAT_TEMP:
					case BAT_SOC:
					case BAT_SOH:
					case CHA_MAX_A:
					case CHA_MAX_V:
					case DIS_MAX_A:
					case DIS_MIN_V:
					case EN_LIMIT:
					case SET_INTERN_DC_RELAY:
					case SET_Analog_CHARGE_Energy:
					case SET_Analog_DISCHARGE_Energy:
					case SET_Analog_DC_CHARGE_Energy:
					case SET_Analog_DC_DISCHARGE_Energy:
						return new IntegerWriteChannel(ess, channelId);
						
					case Serial:
					case Model:
					case Manufacturer:
					case Model_2:
					case Version:
					case Serial_Number:
						
						return new StringReadChannel(ess, channelId);
//-----------------------------------STATES--------------------------------------------------
					case Sinexcel_STATE_1:
					case Sinexcel_STATE_2:
					case Sinexcel_STATE_3:
					case Sinexcel_STATE_4:
					case Sinexcel_STATE_5:
					case Sinexcel_STATE_6:
					case Sinexcel_STATE_7:
					case Sinexcel_STATE_8:
					case Sinexcel_STATE_9:	
						return new StateChannel(ess, channelId);
//-----------------------------------EVENT BITFIELD32-----------------------------------------						
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
					case STATE_11:
					case STATE_12:
					case STATE_13:
					case STATE_14:
					case STATE_15:
//--------------------------------------------FAULT LIST-----------------------------------------
					case STATE_16:
					case STATE_17:
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
						return new StateChannel(ess, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
