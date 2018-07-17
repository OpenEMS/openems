package com.ed.openems.centurio.ess;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class EssUtils {

	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(CenturioEss c) {
		// TODO Auto-generated method stub
		return Stream.of(Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			case STATE:
				return new StateCollectorChannel(c, channelId);
			}
			return null;
		}),Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			case GRID_MODE:
				return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.UNDEFINED.ordinal());
			case SOC:
			case ACTIVE_POWER:
			case REACTIVE_POWER:
				return new IntegerReadChannel(c, channelId);
			case MAX_ACTIVE_POWER:
				return new IntegerReadChannel(c, channelId, CenturioEss.MAX_APPARENT_POWER);
			default:
				break;
			}
			return null;
		}), Arrays.stream(CenturioEss.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			case A001:
			case A002:
			case A003:
			case A004:
			case A005:
			case A010:
			case A021:
			case A022:
			case A030:
			case A032:
			case A040:
			case A050:
			case A060:
			case A071:
			case A072:
			case A100:
			case A110:
			case A200:
			case A210:
			case A220:
			case A230:
			case E001:
			case E002:
			case E010:
			case E021:
			case E022:
			case E030:
			case E041:
			case E042:
			case E050:
			case E060:
			case E070:
			case E080:
			case E101:
			case E102:
			case E103:
			case E104:
			case E110:
			case E120:
			case E140:
			case E150:
			case E160:
			case E170:
			case E180:
				return new StateChannel(c, channelId);

			}
			return null;
		}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			case DEBUG_SET_ACTIVE_POWER:
			case DEBUG_SET_REACTIVE_POWER:
				return new IntegerReadChannel(c, channelId);
			}
			return null;
		})).flatMap(channel -> channel);
	}

}
