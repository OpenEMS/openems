package io.openems.edge.core.componentmanager;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(ComponentManagerImpl c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(ComponentManagerImpl.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CONFIG_NOT_ACTIVATED:
						return new StateChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
