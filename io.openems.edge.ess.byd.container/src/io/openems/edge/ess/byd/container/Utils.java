package io.openems.edge.ess.byd.container;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.byd.container.EssFeneconBydContainer.ChannelId;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssFeneconBydContainer c) {
		return Stream.of(Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
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
			case GRID_MODE:
			case MAX_APPARENT_POWER:
				return new IntegerReadChannel(c, channelId);
			}
			return null;
		}), Arrays.stream(EssFeneconBydContainer.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			//RTU
			case RTU_SYSTEM_WORKSTATE:
			case RTU_SYSTEM_WORKMODE:
			case DISCHARGE_LIMIT_ACTIVE_POWER:
			case CHARGE_LIMIT_ACTIVE_POWER:
			case INDUCTIVE_REACTIVE_POWER:
			case CAPACITIVE_REACTIVE_POWER:
			case CONTAINER_RUN_NUMBER:
				return new IntegerReadChannel(c, channelId);
			case SET_SYSTEM_WORKSTATE:	
			case SET_ACTIVE_POWER_CONTROL:	
			case SET_REACTIVE_POWER_CONTROL:
				return new IntegerWriteChannel(c, channelId);
			//PCS
			case PCS_SYSTEM_WORKSTATE:
			case PCS_SYSTEM_WORKMODE:
			case PHASE3_ACTIVE_POWER:
			case PHASE3_REACTIVE_POWER:
			case PHASE3_INSPECTING_POWER:
			case PCS_DISCHARGE_LIMIT_ACTIVE_POWER:
			case PCS_CHARGE_LIMIT_ACTIVE_POWER:
			case POSITIVE_REACTIVE_POWER_LIMIT:
			case NEGATIVE_REACTIVE_POWER_LIMIT:
			case CURRENT_L1:
			case CURRENT_L2:
			case CURRENT_L3:
			case VOLTAGE_L1:
			case VOLTAGE_L2:
			case VOLTAGE_L3:
			case VOLTAGE_L12:
			case VOLTAGE_L23:
			case VOLTAGE_L31:
			case SYSTEM_FREQUENCY:
			case DC_VOLTAGE:
			case DC_CURRENT:
			case DC_POWER:
			case IGBT_TEMPERATURE_L1:
			case IGBT_TEMPERATURE_L2:
			case IGBT_TEMPERATURE_L3:
			case PCS_WARNING_0:
			case PCS_WARNING_1:
			case PCS_WARNING_2:
			case PCS_WARNING_3:
			case PCS_FAULTS_0:
			case PCS_FAULTS_1:
			case PCS_FAULTS_2:
			case PCS_FAULTS_3:
			case PCS_FAULTS_4:
			case PCS_FAULTS_5:	
				return new IntegerReadChannel(c, channelId);
			//BECU	
			case BATTERY_STRING_WORK_STATE:
			case BATTERY_STRING_TOTAL_VOLTAGE:
			case BATTERY_STRING_CURRENT:
			case BATTERY_STRING_SOC:
			case BATTERY_STRING_AVERAGE_TEMPERATURE:
			case BATTERY_NUMBER_MAX_STRING_VOLTAGE:
			case BATTERY_STRING_MAX_VOLTAGE:
			case BATTERY_STRING_MAX_VOLTAGE_TEMPARATURE:
			case BATTERY_NUMBER_MIN_STRING_VOLTAGE:
			case BATTERY_STRING_MIN_VOLTAGE:
			case BATTERY_STRING_MIN_VOLTAGE_TEMPARATURE:
			case BATTERY_NUMBER_MAX_STRING_TEMPERATURE:
			case BATTERY_STRING_MAX_TEMPERATURE:
			case BATTERY_STRING_MAX_TEMPARATURE_VOLTAGE:
			case BATTERY_NUMBER_MIN_STRING_TEMPERATURE:
			case BATTERY_STRING_MIN_TEMPERATURE:
			case BATTERY_STRING_MIN_TEMPARATURE_VOLTAGE:
			case BATTERY_STRING_CHARGE_CURRENT_LIMIT:
			case BATTERY_STRING_DISCHARGE_CURRENT_LIMIT:
			case BATTERY_STRING_HISTORICAL_LOWEST_CHARGE_CAPACITY:
			case BATTERY_STRING_HISTORICAL_HIGHEST_CHARGE_CAPACITY:
			case BATTERY_STRING_HISTORICAL_LOWEST_DISCHARGE_CAPACITY:
			case BATTERY_STRING_HISTORICAL_HIGHEST_DISCHARGE_CAPACITY:
			case BATTERY_STRING_WARNING_0_0:
			case BATTERY_STRING_WARNING_0_1:
			case BATTERY_STRING_WARNING_1_0:
			case BATTERY_STRING_WARNING_1_1:	
				return new IntegerReadChannel(c, channelId);
			//ADAS	
			case CONTAINER_IMMERSION_STATE_1:
			case CONTAINER_IMMERSION_STATE_0:
			case CONTAINER_FIRE_STATUS_1:
			case CONTAINER_FIRE_STATUS_0:
			case CONTROL_CABINET_STATE_1:
			case CONTROL_CABINET_STATE_0:
			case CONTAINER_GROUNDING_FAULT_1:
			case CONTAINER_GROUNDING_FAULT_0:
			case CONTAINER_DOOR_STATUS_0_1:
			case CONTAINER_DOOR_STATUS_0_0:
			case CONTAINER_DOOR_STATUS_1_1:
			case CONTAINER_DOOR_STATUS_1_0:
			case CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_1:
			case CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE_0:
				return new StateChannel(c, channelId);
			case ADAS_WARNING_0_0:
			case ADAS_WARNING_0_1:
			case ADAS_WARNING_0_2:
			case ADAS_WARNING_1_0:
			case ADAS_WARNING_1_1:
			case ADAS_WARNING_1_2:
			case ADAS_WARNING_1_3:
				return new IntegerReadChannel(c, channelId);
			}
			return null;
		})).flatMap(channel -> channel);
	}
}
