package io.openems.edge.ess.fenecon.mini;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssFeneconMini c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new IntegerReadChannel(c, channelId);
					case MAX_ACTIVE_POWER:
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(EssFeneconMini.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SYSTEM_STATE:
					case ACTIVE_POWER:
					case ALLOWED_CHARGE:
					case ALLOWED_DISCHARGE:
					case BATTERY_CURRENT:
					case BATTERY_GROUP_STATE:
					case BATTERY_POWER:
					case BATTERY_SOC:
					case BATTERY_VOLTAGE:
					case CONTROL_MODE:
					case CURRENT:
					case FREQUENCY:
					case PCS_OPERATION_STATE:
					case PHASE_ALLOWED_APPARENT:
					case REACTIVE_POWER:
					case TOTAL_BATTERY_CHARGE_ENERGY:
					case TOTAL_BATTERY_DISCHARGE_ENERGY:
					case VOLTAGE:
						return new IntegerReadChannel(c, channelId);

					case PCS_MODE:
					case RTC_DAY:
					case RTC_HOUR:
					case RTC_MINUTE:
					case RTC_MONTH:
					case RTC_SECOND:
					case RTC_YEAR:
					case SETUP_MODE:
					case SET_ACTIVE_POWER:
					case SET_PCS_MODE:
					case SET_REACTIVE_POWER:
					case SET_SETUP_MODE:
					case SET_WORK_STATE:
						return new IntegerWriteChannel(c, channelId);

					case STATE_0:
					case STATE_1:
					case STATE_2:
					case STATE_3:
					case STATE_4:
					case STATE_5:
					case STATE_6:
					case STATE_7:
					case STATE_8:
					case STATE_9:
					case STATE_10:
					case STATE_11:
					case STATE_12:
					case STATE_13:
					case STATE_14:
					case STATE_15:
					case STATE_16:
					case STATE_17:
					case STATE_18:
					case STATE_19:
					case STATE_20:
					case STATE_21:
					case STATE_22:
					case STATE_23:
					case STATE_24:
					case STATE_25:
					case STATE_26:
					case STATE_27:
					case STATE_28:
					case STATE_29:
					case STATE_30:
					case STATE_31:
					case STATE_32:
					case STATE_33:
					case STATE_34:
					case STATE_35:
					case STATE_36:
					case STATE_37:
					case STATE_38:
					case STATE_39:
					case STATE_40:
					case STATE_41:
					case STATE_42:
					case STATE_43:
					case STATE_44:
					case STATE_45:
					case STATE_46:
					case STATE_47:
					case STATE_48:
					case STATE_49:
					case STATE_50:
					case STATE_51:
					case STATE_52:
					case STATE_53:
					case STATE_54:
					case STATE_55:
					case STATE_56:
					case STATE_57:
					case STATE_58:
					case STATE_59:
					case STATE_60:
					case STATE_61:
					case STATE_62:
					case STATE_63:
					case STATE_64:
						return new StateChannel(c, channelId);

					}
					return null;
				})).flatMap(channel -> channel);
	}
}
