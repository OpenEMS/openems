package io.openems.edge.ess.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.core.power.ChocoPower;
import io.openems.edge.ess.power.api.Power;

public class EssClusterDummy extends AbstractOpenemsComponent implements ManagedAsymmetricEss, MetaEss {

	private final List<ManagedSymmetricEss> managedEsss = new ArrayList<>();
	private ChocoPower power;

	public EssClusterDummy(SymmetricEss... esss) {
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
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(this, channelId);
					case GRID_MODE:
						return new IntegerReadChannel(this, channelId, SymmetricEss.GridMode.UNDEFINED);
					}
					return null;
				}), Arrays.stream(AsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(ManagedAsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER_L1:
					case DEBUG_SET_ACTIVE_POWER_L2:
					case DEBUG_SET_ACTIVE_POWER_L3:
					case DEBUG_SET_REACTIVE_POWER_L1:
					case DEBUG_SET_REACTIVE_POWER_L2:
					case DEBUG_SET_REACTIVE_POWER_L3:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));

		/*
		 * Add all ManagedSymmetricEss devices to this.managedEsss
		 */
		for (SymmetricEss ess : esss) {
			if (ess instanceof ManagedSymmetricEss) {
				this.managedEsss.add((ManagedSymmetricEss) ess);
			}
		}
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

	@Override
	public List<ManagedSymmetricEss> getEsss() {
		return this.managedEsss;
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		throw new IllegalArgumentException("EssClusterImpl.applyPower() should never be called.");
	}

	public void addToPower(ChocoPower power) {
		this.power = power;
		power.addEss(this);
	}

	@Override
	public Power getPower() {
		return this.power;
	}
}
