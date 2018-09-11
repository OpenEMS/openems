package io.openems.edge.ess.api;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.core.power.ChocoPower;
import io.openems.edge.ess.power.api.Power;

public abstract class ManagedAsymmetricEssDummy extends AbstractOpenemsComponent implements ManagedAsymmetricEss {

	private ChocoPower power;

	public ManagedAsymmetricEssDummy() {
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
	}

	public ManagedAsymmetricEssDummy maxApparentPower(int value) {
		this.getMaxApparentPower().setNextValue(value);
		this.getMaxApparentPower().nextProcessImage();
		return this;
	}

	public ManagedAsymmetricEssDummy allowedCharge(int value) {
		this.getAllowedCharge().setNextValue(value);
		this.getAllowedCharge().nextProcessImage();
		return this;
	}

	public ManagedAsymmetricEssDummy allowedDischarge(int value) {
		this.getAllowedDischarge().setNextValue(value);
		this.getAllowedDischarge().nextProcessImage();
		return this;
	}
	
	private int precision = 1;
	
	public ManagedAsymmetricEssDummy precision(int value) {
		this.precision = value;
		return this;
	}

	@Override
	public int getPowerPrecision() {
		return this.precision;
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

	public void addToPower(ChocoPower power) {
		this.power = power;
		power.addEss(this);
	}

	@Override
	public Power getPower() {
		return this.power;
	}
}
