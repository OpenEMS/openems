package io.openems.edge.common.meta;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.common.OpenemsConstants;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(Meta c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Meta.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case VERSION:
						return new StringReadChannel(c, channelId, OpenemsConstants.OPENEMS_VERSION);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
