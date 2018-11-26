package io.openems.edge.ess.test;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated ManagedSymmetricEss component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricEss extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private final Power power;

	public DummyManagedSymmetricEss(String id) {
		this.power = new DummyPower(MAX_APPARENT_POWER);
		Stream.of(//
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
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new IntegerReadChannel(this, channelId);
					case GRID_MODE:
						return new IntegerReadChannel(this, channelId, SymmetricEss.GridMode.ON_GRID.ordinal());
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(this, channelId, MAX_APPARENT_POWER);
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
						return new IntegerReadChannel(this, channelId);
					case SET_ACTIVE_POWER_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
						return new IntegerWriteChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> {
					channel.nextProcessImage();
					this.addChannel(channel);
				});
		super.activate(null, "", id, true);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
