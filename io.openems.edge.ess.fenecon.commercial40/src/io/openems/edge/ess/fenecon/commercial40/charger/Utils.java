package io.openems.edge.ess.fenecon.commercial40.charger;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssDcChargerFeneconCommercial40 c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssDcCharger.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DC_ACTUAL_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssDcChargerFeneconCommercial40.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case PV_DCDC0_INPUT_POWER:
					case PV_DCDC1_INPUT_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
