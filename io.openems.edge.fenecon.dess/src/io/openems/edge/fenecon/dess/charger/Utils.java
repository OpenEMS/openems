package io.openems.edge.fenecon.dess.charger;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(AbstractFeneconDessCharger c) {
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
//				}), Arrays.stream(FeneconDessCharger.ChannelId.values()).map(channelId -> {
//					switch (channelId) {
//					}
//					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
