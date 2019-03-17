package io.openems.edge.evcs.keba.kecontact;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(KebaKeContact c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Evcs.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CHARGE_POWER:
					case HARDWARE_POWER_LIMIT:
						return new IntegerReadChannel(c, channelId);
					case SET_CHARGE_POWER:
						return new IntegerWriteChannel(c, channelId);
					case SET_DISPLAY_TEXT:
						return new StringWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(KebaKeContact.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTUAL_POWER:
					case COS_PHI:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case CURR_FAILSAFE:
					case CURR_TIMER:
					case CURR_USER:
					case ENERGY_LIMIT:
					case ENERGY_SESSION:
					case ENERGY_TOTAL:
					case ERROR_1:
					case ERROR_2:
					case MAX_CURR:
					case MAX_CURR_PERCENT:
					case TIMEOUT_CT:
					case TIMEOUT_FAILSAFE:
					case VOLTAGE_L1:
					case VOLTAGE_L2:
					case VOLTAGE_L3:
					case PHASES:
						return new IntegerReadChannel(c, channelId);
					case PLUG:
						return new EnumReadChannel(c, channelId, Plug.UNDEFINED);
					case STATUS:
						return new EnumReadChannel(c, channelId, Status.UNDEFINED);
					case ENABLE_USER:
					case ENABLE_SYS:
					case INPUT:
					case OUTPUT:
						return new BooleanReadChannel(c, channelId);
					case COM_MODULE:
					case FIRMWARE:
					case SERIAL:
					case PRODUCT:
						return new StringReadChannel(c, channelId);
					case SET_ENABLED:
						return new BooleanWriteChannel(c, channelId);
					case ChargingStation_COMMUNICATION_FAILED:
						return new StateChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
