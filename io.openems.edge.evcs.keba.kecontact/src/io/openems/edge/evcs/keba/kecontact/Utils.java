package io.openems.edge.evcs.keba.kecontact;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KebaKeContact c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				})
		// , Arrays.stream(KebaKeContact.ChannelId.values()).map(channelId -> {
		// switch (channelId) {
		// case DIGITAL_OUTPUT_1:
		// case DIGITAL_OUTPUT_2:
		// case DIGITAL_OUTPUT_3:
		// case DIGITAL_OUTPUT_4:
		// case DIGITAL_OUTPUT_5:
		// case DIGITAL_OUTPUT_6:
		// case DIGITAL_OUTPUT_7:
		// case DIGITAL_OUTPUT_8:
		// return new BooleanWriteChannel(c, channelId);
		// case DEBUG_DIGITAL_OUTPUT_1:
		// case DEBUG_DIGITAL_OUTPUT_2:
		// case DEBUG_DIGITAL_OUTPUT_3:
		// case DEBUG_DIGITAL_OUTPUT_4:
		// case DEBUG_DIGITAL_OUTPUT_5:
		// case DEBUG_DIGITAL_OUTPUT_6:
		// case DEBUG_DIGITAL_OUTPUT_7:
		// case DEBUG_DIGITAL_OUTPUT_8:
		// return new BooleanReadChannel(c, channelId);
		// }
		// return null;
		// }) //
		).flatMap(channel -> channel);
	}
}
