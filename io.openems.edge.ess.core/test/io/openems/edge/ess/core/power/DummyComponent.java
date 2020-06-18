package io.openems.edge.ess.core.power;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

// TODO replace with DummyManagedSymmetricEss
public abstract class DummyComponent<T> extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	private final String id;

	private PowerComponent power;

	public DummyComponent(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values() //
		);
		this.id = id;
	}

	public T maxApparentPower(int value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		return this.self();
	}

	public T allowedChargePower(int value) {
		this._setAllowedChargePower(value);
		this.getAllowedChargePowerChannel().nextProcessImage();
		return this.self();
	}

	public T allowedDischargePower(int value) {
		this._setAllowedDischargePower(value);
		this.getAllowedDischargePowerChannel().nextProcessImage();
		return this.self();
	}

	public T soc(int value) {
		this._setSoc(value);
		this.getSocChannel().nextProcessImage();
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
