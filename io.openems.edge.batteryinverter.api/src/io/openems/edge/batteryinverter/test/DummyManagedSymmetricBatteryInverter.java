package io.openems.edge.batteryinverter.test;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

/**
 * Provides a simple, simulated {@link ManagedSymmetricBatteryInverter}
 * component that can be used together with the OpenEMS Component test
 * framework.
 */
public class DummyManagedSymmetricBatteryInverter
		extends AbstractDummyOpenemsComponent<DummyManagedSymmetricBatteryInverter>
		implements ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent, StartStoppable {

	public DummyManagedSymmetricBatteryInverter(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values() //
		);
	}

	@Override
	protected DummyManagedSymmetricBatteryInverter self() {
		return this;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this.withStartStop(value);
	}

	/**
	 * Set {@link StartStoppable.ChannelId#START_STOP}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withStartStop(StartStop value) {
		withValue(this, StartStoppable.ChannelId.START_STOP, value);
		return this;
	}

	/**
	 * Set {@link SymmetricBatteryInverter.ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withMaxApparentPower(int value) {
		withValue(this, SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER, value);
		return this;
	}

	/**
	 * Set {@link SymmetricBatteryInverter.ChannelId#ACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withActivePower(Integer value) {
		withValue(this, SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricBatteryInverter.ChannelId#REACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withReactivePower(Integer value) {
		withValue(this, SymmetricBatteryInverter.ChannelId.REACTIVE_POWER, value);
		return this.self();
	}

	/**
	 * Set {@link SymmetricBatteryInverter.ChannelId#DC_MIN_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withDcMinVoltage(int value) {
		withValue(this, SymmetricBatteryInverter.ChannelId.DC_MIN_VOLTAGE, value);
		return this;
	}

	/**
	 * Set {@link SymmetricBatteryInverter.ChannelId#DC_MAX_VOLTAGE}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withDcMaxVoltage(int value) {
		withValue(this, SymmetricBatteryInverter.ChannelId.DC_MAX_VOLTAGE, value);
		return this;
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		this._setActivePower(setActivePower);
		this._setReactivePower(setReactivePower);
	}
}
