package io.openems.edge.scheduler.allalphabetically;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(AllAlphabetically c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
//				}), Arrays.stream(AllAlphabetically.ChannelId.values()).map(channelId -> {
//					switch (channelId) {
//					}
//					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
