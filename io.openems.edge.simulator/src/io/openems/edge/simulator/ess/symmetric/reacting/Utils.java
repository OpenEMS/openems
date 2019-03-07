package io.openems.edge.simulator.ess.symmetric.reacting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(OpenemsComponent c) {
		List<AbstractReadChannel<?>> result = new ArrayList<>();
		for (io.openems.edge.common.component.OpenemsComponent.ChannelId channelId : OpenemsComponent.ChannelId
				.values()) {
			switch (channelId) {
			case STATE:
				result.add(new StateCollectorChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.SymmetricEss.ChannelId channelId : SymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case SOC:
			case ACTIVE_POWER:
			case REACTIVE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case MAX_APPARENT_POWER:
				result.add(new IntegerReadChannel(c, channelId, 3000));
				break;
			case GRID_MODE:
				result.add(new EnumReadChannel(c, channelId, GridMode.UNDEFINED));
				break;
			case ACTIVE_DISCHARGE_ENERGY:
			case ACTIVE_CHARGE_ENERGY:
				result.add(new LongReadChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId channelId : ManagedSymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case DEBUG_SET_ACTIVE_POWER:
			case DEBUG_SET_REACTIVE_POWER:
			case ALLOWED_CHARGE_POWER:
			case ALLOWED_DISCHARGE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case SET_ACTIVE_POWER_EQUALS:
			case SET_REACTIVE_POWER_EQUALS:
			case SET_ACTIVE_POWER_LESS_OR_EQUALS:
			case SET_ACTIVE_POWER_GREATER_OR_EQUALS:
			case SET_REACTIVE_POWER_LESS_OR_EQUALS:
			case SET_REACTIVE_POWER_GREATER_OR_EQUALS:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			case APPLY_POWER_FAILED:
				result.add(new StateChannel(c, channelId));
				break;
			}
		}
		return result.stream();
	}
}
