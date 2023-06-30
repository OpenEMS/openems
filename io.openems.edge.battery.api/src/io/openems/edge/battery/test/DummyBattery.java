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
		this(id, new io.openems.edge.common.channel.ChannelId[0]);
	}

	public DummyBattery(String id, io.openems.edge.common.channel.ChannelId[] additionalChannelIds) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				additionalChannelIds //
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

	/**
	 * Sets and applies the {@link Battery.ChannelId#CAPACITY}.
	 *
	 * @param value the Capacity in [Wh]
	 * @return myself
	 */
	public DummyBattery withCapacity(int value) {
		this._setCapacity(value);
		this.getCapacityChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#VOLTAGE}.
	 *
	 * @param value the Capacity in [V]
	 * @return myself
	 */
	public DummyBattery withVoltage(int value) {
		this._setVoltage(value);
		this.getVoltageChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @param value the Discharge Max Current in [A]
	 * @return myself
	 */
	public DummyBattery withDischargeMaxCurrent(int value) {
		this._setDischargeMaxCurrent(value);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @param value the Charge Max Current in [A]
	 * @return myself
	 */
	public DummyBattery withChargeMaxCurrent(int value) {
		this._setChargeMaxCurrent(value);
		this.getChargeMaxCurrentChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#MIN_CELL_VOLTAGE}.
	 *
	 * @param value the Min-Cell-Voltage in [mV]
	 * @return myself
	 */
	public DummyBattery withMinCellVoltage(int value) {
		this._setMinCellVoltage(value);
		this.getMinCellVoltageChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#MAX_CELL_VOLTAGE}.
	 *
	 * @param value the Max-Cell-Voltage in [mV]
	 * @return myself
	 */
	public DummyBattery withMaxCellVoltage(int value) {
		this._setMaxCellVoltage(value);
		this.getMaxCellVoltageChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#MIN_CELL_TEMPERATURE}.
	 *
	 * @param value the Min-Cell-Temperature in [degC]
	 * @return myself
	 */
	public DummyBattery withMinCellTemperature(int value) {
		this._setMinCellTemperature(value);
		this.getMinCellTemperatureChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Battery.ChannelId#MAX_CELL_TEMPERATURE}.
	 *
	 * @param value the Max-Cell-Temperature in [degC]
	 * @return myself
	 */
	public DummyBattery withMaxCellTemperature(int value) {
		this._setMaxCellTemperature(value);
		this.getMaxCellTemperatureChannel().nextProcessImage();
		return this;
	}

}
