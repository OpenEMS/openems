package io.openems.edge.battery.soltaro;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
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

		setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		setSoc(DEFAULT_SOC);
		setCapacity(DEFAULT_CAPACITY);
		setVoltage(DEFAULT_VOLTAGE);
		setChargeMaxCurrent(DEFAULT_MAX_CHARGE_CURRENT);
		setDischargeMaxCurrent(DEFAULT_MAX_DISCHARGE_CURRENT);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this._setMinCellVoltage(minimalCellVoltage);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this._setMinCellVoltage(null);
		this.getMinCellVoltageChannel().nextProcessImage();
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this._setMaxCellVoltage(maximalCellVoltage);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	public void setMaximalCellVoltageToUndefined() {
		this._setMaxCellVoltage(null);
		this.getMaxCellVoltageChannel().nextProcessImage();
	}

	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this._setMinCellTemperature(minimalCellTemperature);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	public void setMinimalCellTemperatureToUndefined() {
		this._setMinCellTemperature(null);
		this.getMinCellTemperatureChannel().nextProcessImage();
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this._setMaxCellTemperature(maximalCellTemperature);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	public void setMaximalCellTemperatureToUndefined() {
		this._setMaxCellTemperature(null);
		this.getMaxCellTemperatureChannel().nextProcessImage();
	}

	public void setSoc(int soc) {
		this._setSoc(soc);
		this.getSocChannel().nextProcessImage();
	}

	public void setSocToUndefined() {
		this._setSoc(null);
		this.getSocChannel().nextProcessImage();
	}
	
	public void setVoltage(int voltage) {
		this._setVoltage(voltage);
		this.getVoltageChannel().nextProcessImage();
	}

	public void setVoltageToUndefined() {
		this._setVoltage(null);
		this.getVoltageChannel().nextProcessImage();
	}
	
	public void setCapacity(int capacity) {
		this._setCapacity(capacity);
		this.getCapacityChannel().nextProcessImage();
	}

	public void setCapacityToUndefined() {
		this._setCapacity(null);
		this.getCapacityChannel().nextProcessImage();
	}
	
	public void setForceDischargeActive(boolean active) {
		this._setForceDischargeActive(active);
		this.getForceDischargeActiveChannel().nextProcessImage();
	}

	public void setForceDischargeActiveToUndefined() {
		this._setForceDischargeActive(null);
		this.getForceDischargeActiveChannel().nextProcessImage();
	}
	
	public void setForceChargeActive(boolean active) {
		this._setForceChargeActive(active);
		this.getForceChargeActiveChannel().nextProcessImage();
	}

	public void setForceChargeActiveToUndefined() {
		this._setForceChargeActive(null);
		this.getForceChargeActiveChannel().nextProcessImage();
	}
	
	public void setChargeMaxCurrent(int value) {
		this._setChargeMaxCurrent(value);
		this.getChargeMaxCurrentChannel().nextProcessImage();
	}

	public void setChargeMaxCurrentToUndefined() {
		this._setChargeMaxCurrent(null);
		this.getChargeMaxCurrentChannel().nextProcessImage();
	}
	
	public void setDischargeMaxCurrent(int value) {
		this._setDischargeMaxCurrent(value);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
	}

	public void setDischargeMaxCurrentToUndefined() {
		this._setDischargeMaxCurrent(null);
		this.getDischargeMaxCurrentChannel().nextProcessImage();
	}
	
	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO start stop is not implemented
		throw new NotImplementedException("Start Stop is not implemented");
	}
}
