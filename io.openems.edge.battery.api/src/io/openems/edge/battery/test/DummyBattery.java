package io.openems.edge.battery.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Provides a simple, simulated {@link Battery} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyBattery extends AbstractOpenemsComponent implements Battery, OpenemsComponent, StartStoppable {

	public DummyBattery(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}

	public DummyBattery withCapacity(int value) {
		this._setCapacity(value);
		this.getCapacityChannel().nextProcessImage();
		return this;
	}

	public DummyBattery withVoltage(int value) {
		this._setVoltage(value);
		this.getVoltageChannel().nextProcessImage();
		return this;
	}

	public DummyBattery withDischargeMaxCurrent(int value) {
		this._setDischargeMaxCurrent(value);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
		return this;
	}

	public DummyBattery withChargeMaxCurrent(int value) {
		this._setChargeMaxCurrent(value);
		this.getChargeMaxCurrentChannel().nextProcessImage();
		return this;
	}

	public DummyBattery withMinCellVoltage(int value) {
		this._setMinCellVoltage(value);
		this.getMinCellVoltageChannel().nextProcessImage();
		return this;
	}

	public DummyBattery withMaxCellVoltage(int value) {
		this._setMaxCellVoltage(value);
		this.getMaxCellVoltageChannel().nextProcessImage();
		return this;
	}

}
