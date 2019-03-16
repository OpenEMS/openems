package io.openems.edge.io.kmtronic;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.BooleanReadChannel;
import io.openems.edge.common.channel.internal.BooleanWriteChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KmtronicRelayOutput c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				})
				// , Arrays.stream(DigitalOutput.ChannelId.values()).map(channelId -> { return
				// * null; })
				, Arrays.stream(KmtronicRelayOutput.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case RELAY_1:
					case RELAY_2:
					case RELAY_3:
					case RELAY_4:
					case RELAY_5:
					case RELAY_6:
					case RELAY_7:
					case RELAY_8:
						return new BooleanWriteChannel(c, channelId);
					case DEBUG_RELAY_1:
					case DEBUG_RELAY_2:
					case DEBUG_RELAY_3:
					case DEBUG_RELAY_4:
					case DEBUG_RELAY_5:
					case DEBUG_RELAY_6:
					case DEBUG_RELAY_7:
					case DEBUG_RELAY_8:
						return new BooleanReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
