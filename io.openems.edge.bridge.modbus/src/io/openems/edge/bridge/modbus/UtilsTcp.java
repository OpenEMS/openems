package io.openems.edge.bridge.modbus;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class UtilsTcp {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(BridgeModbusTcpImpl s) {
		return Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(s, channelId);
					}
					return null;
				}), Arrays.stream(AbstractModbusBridge.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SLAVE_COMMUNICATION_FAILED:
						return new StateChannel(s, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
