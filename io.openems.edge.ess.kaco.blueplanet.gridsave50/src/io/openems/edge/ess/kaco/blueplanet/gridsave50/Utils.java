package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssKacoBlueplanetGridsave50 c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					case MAX_ACTIVE_POWER:
						return new IntegerReadChannel(c, channelId, EssKacoBlueplanetGridsave50.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, Ess.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssKacoBlueplanetGridsave50.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case CONN:
					case CHA_CUTOFF_A:
					case CHA_MAX_A:
					case CHA_MAX_V:
					case DIS_CUTOFF_A:
					case DIS_MAX_A:
					case DIS_MIN_V:
					case EN_LIMIT:
					case W_SET_ENA:
					case W_SET_PCT:
						return new IntegerWriteChannel(c, channelId);
					case A_SF:
					case V_SF:
					case STATE_POWER_UNIT:
					case VENDOR_OPERATING_STATE:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}) //
		).flatMap(channel -> channel);
	}
}
