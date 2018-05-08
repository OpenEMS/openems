package io.openems.edge.timedata.influxdb;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(InfluxTimedata c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
					// }), Arrays.stream(Timedata.ChannelId.values()).map(channelId -> {
					// switch (channelId) {
					// }
					// return null;
				}), Arrays.stream(InfluxTimedata.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_0:
					case STATE_1:
						return new BooleanReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
