package io.openems.edge.battery.api;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public class DummyBattery extends AbstractOpenemsComponent implements Battery, StartStoppable {

	public static final int DEFAULT_SOC = 50;
	public static final int DEFAULT_CAPACITY = 50_000;
	public static final int DEFAULT_VOLTAGE = 750;
	public static final int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static final int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static final int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static final int DEFAULT_MAX_CELL_TEMPERATURE = 33;
	public static final int DEFAULT_MAX_CHARGE_CURRENT = 50;
	public static final int DEFAULT_MAX_DISCHARGE_CURRENT = 50;

	protected DummyBattery(//
	) { //
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values() //
		);

		this.setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		this.setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		this.setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		this.setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		this.setSoc(DEFAULT_SOC);
		this.setCapacity(DEFAULT_CAPACITY);
		this.setVoltage(DEFAULT_VOLTAGE);
		this.setChargeMaxCurrent(DEFAULT_MAX_CHARGE_CURRENT);
		this.setDischargeMaxCurrent(DEFAULT_MAX_DISCHARGE_CURRENT);
	}

	void setMinimalCellVoltage(int minimalCellVoltage) {
		this._setMinCellVoltage(minimalCellVoltage);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	void setMinimalCellVoltageToUndefined() {
		this._setMinCellVoltage(null);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	void setMaximalCellVoltage(int maximalCellVoltage) {
		this._setMaxCellVoltage(maximalCellVoltage);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	void setMaximalCellVoltageToUndefined() {
		this._setMaxCellVoltage(null);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	void setMinimalCellTemperature(int minimalCellTemperature) {
		this._setMinCellTemperature(minimalCellTemperature);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	void setMinimalCellTemperatureToUndefined() {
		this._setMinCellTemperature(null);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	void setMaximalCellTemperature(int maximalCellTemperature) {
		this._setMaxCellTemperature(maximalCellTemperature);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	void setMaximalCellTemperatureToUndefined() {
		this._setMaxCellTemperature(null);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	void setSoc(int soc) {
		this._setSoc(soc);
		this.getSocChannel().nextProcessImage();
	}

	void setSocToUndefined() {
		this._setSoc(null);
		this.getSocChannel().nextProcessImage();
	}

	void setVoltage(int voltage) {
		this._setVoltage(voltage);
		this.getVoltageChannel().nextProcessImage();
	}

	void setVoltageToUndefined() {
		this._setVoltage(null);
		this.getVoltageChannel().nextProcessImage();
	}

	void setCapacity(int capacity) {
		this._setCapacity(capacity);
		this.getCapacityChannel().nextProcessImage();
	}

	void setCapacityToUndefined() {
		this._setCapacity(null);
		this.getCapacityChannel().nextProcessImage();
	}

	void setForceDischargeActive(boolean active) {
		this._setForceDischargeActive(active);
		this.getForceDischargeActiveChannel().nextProcessImage();
	}

	void setForceDischargeActiveToUndefined() {
		this._setForceDischargeActive(null);
		this.getForceDischargeActiveChannel().nextProcessImage();
	}

	void setForceChargeActive(boolean active) {
		this._setForceChargeActive(active);
		this.getForceChargeActiveChannel().nextProcessImage();
	}

	void setForceChargeActiveToUndefined() {
		this._setForceChargeActive(null);
		this.getForceChargeActiveChannel().nextProcessImage();
	}

	void setChargeMaxCurrent(int value) {
		this._setChargeMaxCurrent(value);
		this.getChargeMaxCurrentChannel().nextProcessImage();
	}

	void setChargeMaxCurrentToUndefined() {
		this._setChargeMaxCurrent(null);
		this.getChargeMaxCurrentChannel().nextProcessImage();
	}

	void setDischargeMaxCurrent(int value) {
		this._setDischargeMaxCurrent(value);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
	}

	void setDischargeMaxCurrentToUndefined() {
		this._setDischargeMaxCurrent(null);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO start stop is not implemented
		throw new NotImplementedException("Start Stop is not implemented");
	}
}
