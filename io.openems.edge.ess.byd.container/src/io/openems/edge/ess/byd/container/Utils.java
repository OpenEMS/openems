package io.openems.edge.ess.byd.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssFeneconBydContainer c) {
		List<AbstractReadChannel<?>> result = new ArrayList<>();
		for (io.openems.edge.common.component.OpenemsComponent.ChannelId channelId : OpenemsComponent.ChannelId
				.values()) {
			switch (channelId) {
			case STATE:
				result.add(new StateCollectorChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.SymmetricEss.ChannelId channelId : SymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case SOC:
			case ACTIVE_POWER:
			case REACTIVE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case MAX_APPARENT_POWER:
				result.add(new IntegerReadChannel(c, channelId, 100000)); // TODO
				break;
			case GRID_MODE:
				result.add(new IntegerReadChannel(c, channelId, GridMode.ON_GRID));
				break;
			case ACTIVE_DISCHARGE_ENERGY:
			case ACTIVE_CHARGE_ENERGY:
				result.add(new LongReadChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId channelId : ManagedSymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case DEBUG_SET_ACTIVE_POWER:
			case DEBUG_SET_REACTIVE_POWER:
			case ALLOWED_CHARGE_POWER:
			case ALLOWED_DISCHARGE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case SET_ACTIVE_POWER_EQUALS:
			case SET_REACTIVE_POWER_EQUALS:
			case SET_ACTIVE_POWER_LESS_OR_EQUALS:
			case SET_ACTIVE_POWER_GREATER_OR_EQUALS:
			case SET_REACTIVE_POWER_LESS_OR_EQUALS:
			case SET_REACTIVE_POWER_GREATER_OR_EQUALS:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			}
		}
		for (EssFeneconBydContainer.ChannelId channelId : EssFeneconBydContainer.ChannelId.values()) {
			switch (channelId) {
			case READ_ONLY_MODE:
				result.add(new StateChannel(c, channelId));
				break;
			// RTU
			case SYSTEM_WORKSTATE:
			case LIMIT_INDUCTIVE_REACTIVE_POWER:
			case LIMIT_CAPACITIVE_REACTIVE_POWER:
			case CONTAINER_RUN_NUMBER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case SYSTEM_WORKMODE:
			case SET_SYSTEM_WORKSTATE:
			case SET_ACTIVE_POWER:
			case SET_REACTIVE_POWER:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			// PCS
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
				result.add(new IntegerReadChannel(c, channelId));
				break;
			// BECU
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
			case BATTERY_STRING_WARNING_0_0:
			case BATTERY_STRING_WARNING_0_1:
			case BATTERY_STRING_WARNING_1_0:
			case BATTERY_STRING_WARNING_1_1:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			// ADAS
			case CONTAINER_IMMERSION_STATE:
			case CONTAINER_FIRE_STATUS:
			case CONTROL_CABINET_STATE:
			case CONTAINER_GROUNDING_FAULT:
			case CONTAINER_DOOR_STATUS_0:
			case CONTAINER_DOOR_STATUS_1:
			case CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case ADAS_WARNING_0_0:
			case ADAS_WARNING_0_1:
			case ADAS_WARNING_0_2:
			case ADAS_WARNING_1_0:
			case ADAS_WARNING_1_1:
			case ADAS_WARNING_1_2:
			case ADAS_WARNING_1_3:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			}
		}
		return result.stream();
	}
}
