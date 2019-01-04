package io.openems.edge.ess.fenecon.commercial40.charger;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssDcChargerFeneconCommercial40 c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssDcCharger.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTUAL_POWER:
					case MAX_ACTUAL_POWER:
						return new IntegerReadChannel(c, channelId);
					case ACTUAL_ENERGY:
						return new LongReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssDcChargerFeneconCommercial40.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case BMS_DCDC0_IGBT_TEMPERATURE:
					case BMS_DCDC0_INPUT_CURRENT:
					case BMS_DCDC0_INPUT_POWER:
					case BMS_DCDC0_INPUT_VOLTAGE:
					case BMS_DCDC0_OUTPUT_CURRENT:
					case BMS_DCDC0_OUTPUT_POWER:
					case BMS_DCDC0_OUTPUT_VOLTAGE:
					case BMS_DCDC0_REACTOR_TEMPERATURE:
					case BMS_DCDC1_IGBT_TEMPERATURE:
					case BMS_DCDC1_INPUT_CURRENT:
					case BMS_DCDC1_INPUT_POWER:
					case BMS_DCDC1_INPUT_VOLTAGE:
					case BMS_DCDC1_OUTPUT_CURRENT:
					case BMS_DCDC1_OUTPUT_POWER:
					case BMS_DCDC1_OUTPUT_VOLTAGE:
					case BMS_DCDC1_REACTOR_TEMPERATURE:
					case PV_DCDC0_IGBT_TEMPERATURE:
					case PV_DCDC0_INPUT_CURRENT:
					case PV_DCDC0_INPUT_POWER:
					case PV_DCDC0_INPUT_VOLTAGE:
					case PV_DCDC0_OUTPUT_CURRENT:
					case PV_DCDC0_OUTPUT_POWER:
					case PV_DCDC0_OUTPUT_VOLTAGE:
					case PV_DCDC0_REACTOR_TEMPERATURE:
					case PV_DCDC1_IGBT_TEMPERATURE:
					case PV_DCDC1_INPUT_CURRENT:
					case PV_DCDC1_INPUT_POWER:
					case PV_DCDC1_INPUT_VOLTAGE:
					case PV_DCDC1_OUTPUT_CURRENT:
					case PV_DCDC1_OUTPUT_POWER:
					case PV_DCDC1_OUTPUT_VOLTAGE:
					case PV_DCDC1_REACTOR_TEMPERATURE:
						return new IntegerReadChannel(c, channelId);
					case BMS_DCDC0_OUTPUT_ENERGY:
					case BMS_DCDC1_INPUT_ENERGY:
					case BMS_DCDC1_OUTPUT_ENERGY:
					case PV_DCDC0_OUTPUT_ENERGY:
					case PV_DCDC1_INPUT_ENERGY:
					case PV_DCDC1_OUTPUT_ENERGY:
					case BMS_DCDC0_INPUT_ENERGY:
					case PV_DCDC0_INPUT_ENERGY:
					case BMS_DCDC0_INPUT_CHARGE_ENERGY:
					case BMS_DCDC0_INPUT_DISCHARGE_ENERGY:
					case BMS_DCDC0_OUTPUT_CHARGE_ENERGY:
					case BMS_DCDC0_OUTPUT_DISCHARGE_ENERGY:
					case BMS_DCDC1_INPUT_CHARGE_ENERGY:
					case BMS_DCDC1_INPUT_DISCHARGE_ENERGY:
					case BMS_DCDC1_OUTPUT_CHARGE_ENERGY:
					case BMS_DCDC1_OUTPUT_DISCHARGE_ENERGY:
					case PV_DCDC0_INPUT_CHARGE_ENERGY:
					case PV_DCDC0_INPUT_DISCHARGE_ENERGY:
					case PV_DCDC0_OUTPUT_CHARGE_ENERGY:
					case PV_DCDC0_OUTPUT_DISCHARGE_ENERGY:
					case PV_DCDC1_INPUT_CHARGE_ENERGY:
					case PV_DCDC1_INPUT_DISCHARGE_ENERGY:
					case PV_DCDC1_OUTPUT_CHARGE_ENERGY:
					case PV_DCDC1_OUTPUT_DISCHARGE_ENERGY:
						return new LongReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
