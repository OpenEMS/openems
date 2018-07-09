package io.openems.edge.ess.api;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.power.api.Power;

public abstract class ManagedSymmetricEssDummy extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	private final Power power;

	public ManagedSymmetricEssDummy() {
		Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case MAX_ACTIVE_POWER:
						return new IntegerReadChannel(this, channelId);
					case GRID_MODE:
						return new IntegerReadChannel(this, channelId, SymmetricEss.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
		this.power = new Power(this);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public String id() {
		return "dummy";
	}

	@Override
	public String servicePid() {
		return "no_service_pid";
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
