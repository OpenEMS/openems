package io.openems.edge.ess.core.power;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

public abstract class DummyComponent<T> extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	private final String id;

	private PowerComponent power;

	public DummyComponent(String id) {
		this.id = id;
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
					case SET_ACTIVE_POWER_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
						return new IntegerWriteChannel(this, channelId);
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
					case SET_ACTIVE_POWER_L1_EQUALS:
					case SET_ACTIVE_POWER_L2_EQUALS:
					case SET_ACTIVE_POWER_L3_EQUALS:
					case SET_REACTIVE_POWER_L1_EQUALS:
					case SET_REACTIVE_POWER_L2_EQUALS:
					case SET_REACTIVE_POWER_L3_EQUALS:
						return new IntegerWriteChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> this.addChannel(channel));
	}

	public T maxApparentPower(int value) {
		this.getMaxApparentPower().setNextValue(value);
		this.getMaxApparentPower().nextProcessImage();
		return this.self();
	}

	public T allowedCharge(int value) {
		this.getAllowedCharge().setNextValue(value);
		this.getAllowedCharge().nextProcessImage();
		return this.self();
	}

	public T allowedDischarge(int value) {
		this.getAllowedDischarge().setNextValue(value);
		this.getAllowedDischarge().nextProcessImage();
		return this.self();
	}

	public T soc(int value) {
		this.getSoc().setNextValue(value);
		this.getSoc().nextProcessImage();
		return this.self();
	}

	private int precision = 1;

	public T precision(int value) {
		this.precision = value;
		return this.self();
	}

	@Override
	public int getPowerPrecision() {
		return this.precision;
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public String servicePid() {
		return "no_service_pid";
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public void addToPower(PowerComponent power) {
		this.power = power;
		power.addEss(this);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	protected abstract T self();
}
