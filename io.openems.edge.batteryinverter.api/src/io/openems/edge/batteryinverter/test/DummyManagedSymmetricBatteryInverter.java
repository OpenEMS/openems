package io.openems.edge.batteryinverter.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Provides a simple, simulated {@link ManagedSymmetricBatteryInverter}
 * component that can be used together with the OpenEMS Component test
 * framework.
 */
public class DummyManagedSymmetricBatteryInverter extends AbstractOpenemsComponent
		implements ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent, StartStoppable {

	public DummyManagedSymmetricBatteryInverter(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}

	/**
	 * Sets and applies the {@link StartStop.ChannelId#START_STOP}.
	 *
	 * @param value the {@link StartStop} state
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withStartStop(StartStop value) {
		this._setStartStop(value);
		this.getStartStopChannel().nextProcessImage();
		return this;
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		this._setActivePower(setActivePower);
		this._setReactivePower(setReactivePower);
	}

	/**
	 * Sets and applies the
	 * {@link SymmetricBatteryInverter.ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @param value the MaxApparentPower in [VA]
	 * @return myself
	 */
	public DummyManagedSymmetricBatteryInverter withMaxApparentPower(int value) {
		this._setMaxApparentPower(value);
		this.getMaxApparentPowerChannel().nextProcessImage();
		return this;
	}

}
