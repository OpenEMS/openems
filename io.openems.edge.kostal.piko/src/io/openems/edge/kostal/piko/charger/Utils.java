package io.openems.edge.kostal.piko.charger;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.IntegerReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KostalPikoCharger c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssDcCharger.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case MAX_ACTUAL_POWER:
					case ACTUAL_POWER:
					case ACTUAL_ENERGY:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
