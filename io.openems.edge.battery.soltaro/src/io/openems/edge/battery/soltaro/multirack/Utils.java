package io.openems.edge.battery.soltaro.multirack;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	
	public static Stream<? extends AbstractReadChannel<?>> initializeMultiRackChannels(MultiRack m) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(m, channelId);
					}
					return null;
				}), Arrays.stream(Battery.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case SOH:
					case CURRENT:
					case MAX_CELL_TEMPERATURE:
					case MAX_CELL_VOLTAGE:
					case MAX_POWER:
					case MIN_CELL_TEMPERATURE:
					case MIN_CELL_VOLTAGE:
					case VOLTAGE:
						return new IntegerReadChannel(m, channelId);
					case CHARGE_MAX_CURRENT:
						return new IntegerReadChannel(m, channelId, MultiRack.CHARGE_MAX_A);
					case CHARGE_MAX_VOLTAGE:
						return new IntegerReadChannel(m, channelId, MultiRack.CHARGE_MAX_V);
					case DISCHARGE_MAX_CURRENT:
						return new IntegerReadChannel(m, channelId, MultiRack.DISCHARGE_MAX_A);
					case DISCHARGE_MIN_VOLTAGE:
						return new IntegerReadChannel(m, channelId, MultiRack.DISCHARGE_MIN_V);
					case READY_FOR_WORKING:
						return new BooleanReadChannel(m, channelId);
					case CAPACITY:
						return new IntegerReadChannel(m, channelId, MultiRack.CAPACITY_KWH);
						
					}
					return null;
				}), Arrays.stream(MultiRackChannelId.values()).map(channelId -> {
					switch (channelId) {
					case START_STOP:
					case RACK_1_USAGE:
					case RACK_2_USAGE:
					case RACK_3_USAGE:
					case RACK_4_USAGE:
					case RACK_5_USAGE:
						
					case EMS_ADDRESS:
					case EMS_COMMUNICATION_TIMEOUT:
						
					case RACK_1_POSITIVE_CONTACTOR:
					case RACK_2_POSITIVE_CONTACTOR:
					case RACK_3_POSITIVE_CONTACTOR:
					case RACK_4_POSITIVE_CONTACTOR:
					case RACK_5_POSITIVE_CONTACTOR:
						
					case SYSTEM_INSULATION_LEVEL_1:
					case SYSTEM_INSULATION_LEVEL_2:
						
						return new IntegerWriteChannel(m, channelId);

					case MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER:
					case MASTER_ALARM_LEVEL_1_INSULATION:
					case MASTER_ALARM_LEVEL_2_INSULATION:
					case MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE:
					case MASTER_ALARM_PCS_EMS_CONTROL_FAIL:
						
					case SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1:
					case SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2:
					case SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3:
					case SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4:
					case SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_5:			
						
					case RACK_1_LEVEL_2_ALARM:
					case RACK_1_PCS_CONTROL_FAULT:
					case RACK_1_COMMUNICATION_WITH_MASTER_ERROR:
					case RACK_1_DEVICE_ERROR:
					case RACK_1_CYCLE_OVER_CURRENT:
					case RACK_1_VOLTAGE_DIFFERENCE:
					
					case RACK_2_LEVEL_2_ALARM:
					case RACK_2_PCS_CONTROL_FAULT:
					case RACK_2_COMMUNICATION_WITH_MASTER_ERROR:
					case RACK_2_DEVICE_ERROR:
					case RACK_2_CYCLE_OVER_CURRENT:
					case RACK_2_VOLTAGE_DIFFERENCE:
					
					case RACK_3_LEVEL_2_ALARM:
					case RACK_3_PCS_CONTROL_FAULT:
					case RACK_3_COMMUNICATION_WITH_MASTER_ERROR:
					case RACK_3_DEVICE_ERROR:
					case RACK_3_CYCLE_OVER_CURRENT:
					case RACK_3_VOLTAGE_DIFFERENCE:
					
					case RACK_4_LEVEL_2_ALARM:
					case RACK_4_PCS_CONTROL_FAULT:
					case RACK_4_COMMUNICATION_WITH_MASTER_ERROR:
					case RACK_4_DEVICE_ERROR:
					case RACK_4_CYCLE_OVER_CURRENT:
					case RACK_4_VOLTAGE_DIFFERENCE:
					
					case RACK_5_LEVEL_2_ALARM:
					case RACK_5_PCS_CONTROL_FAULT:
					case RACK_5_COMMUNICATION_WITH_MASTER_ERROR:
					case RACK_5_DEVICE_ERROR:
					case RACK_5_CYCLE_OVER_CURRENT:
					case RACK_5_VOLTAGE_DIFFERENCE:
						return new StateChannel(m, channelId);
					
					case CHARGE_INDICATION:
					case CURRENT:					
					case SYSTEM_RUNNING_STATE:						
					case VOLTAGE:
					case STATE_MACHINE:
					case SYSTEM_INSULATION:
						return new IntegerReadChannel(m, channelId);
	
										
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
