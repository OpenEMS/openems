package io.openems.edge.project.controller.enbag.emergencymode;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

/**
 * Simple cluster wrapper
 */
class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EmergencyClusterMode c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(Controller.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case RUN_FAILED:
						return new StateChannel(c, channelId);
					}
					return null;
				}), //
				Arrays.stream(EmergencyClusterMode.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_INVERTER:
						return new EnumReadChannel(c, channelId, PvState.UNDEFINED);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
