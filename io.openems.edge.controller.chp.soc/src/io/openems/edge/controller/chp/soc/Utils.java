package io.openems.edge.controller.chp.soc;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.StateChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.EnumReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(ControllerChpSoc c) {
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
				Arrays.stream(ControllerChpSoc.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_MACHINE:
						return new EnumReadChannel(c, channelId, State.UNDEFINED);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
