package io.openems.edge.core.meta;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.common.OpenemsConstants;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(MetaImpl c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Meta.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case VERSION:
						return new StringReadChannel(c, channelId, OpenemsConstants.VERSION);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
