package io.openems.edge.controller.dischargelimitconsideringcellvoltage;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.StateChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.EnumReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.dischargelimitconsideringcellvoltage.DischargeLimitConsideringCellVoltage.State;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(DischargeLimitConsideringCellVoltage c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
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
				Arrays.stream(DischargeLimitConsideringCellVoltage.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE_MACHINE:
						return new EnumReadChannel(c, channelId, State.UNDEFINED);

					default:
						break;
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
