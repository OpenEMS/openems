package io.openems.edge.io.kmtronic;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KmtronicRelayOutput c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				})
				// , Arrays.stream(DigitalOutput.ChannelId.values()).map(channelId -> { return
				// * null; })
				, Arrays.stream(KmtronicRelayOutput.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DIGITAL_OUTPUT_1:
					case DIGITAL_OUTPUT_2:
					case DIGITAL_OUTPUT_3:
					case DIGITAL_OUTPUT_4:
					case DIGITAL_OUTPUT_5:
					case DIGITAL_OUTPUT_6:
					case DIGITAL_OUTPUT_7:
					case DIGITAL_OUTPUT_8:
						return new BooleanReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
