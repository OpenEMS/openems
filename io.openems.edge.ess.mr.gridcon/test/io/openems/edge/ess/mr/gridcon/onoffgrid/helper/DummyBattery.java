package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;

public class DummyBattery extends AbstractOpenemsComponent implements Battery {

	public static int DEFAULT_SOC = 50;
	public static int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static int DEFAULT_MAX_CELL_TEMPERATURE = 33;

	public static int DEFAULT_VOLTAGE = 800;
	public static int DEFAULT_MAX_CHARGE_CURRENT = 80;
	public static int DEFAULT_MAX_DISCHARGE_CURRENT = 60;

	private boolean running = false;
	private boolean error = false;

	public DummyBattery(//
	) { //
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values() //
		);

		setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		setSoc(DEFAULT_SOC);
		setVoltage(DEFAULT_VOLTAGE);
		setMaximalChargeCurrent(DEFAULT_MAX_CHARGE_CURRENT);
		setMaximalDischargeCurrent(DEFAULT_MAX_DISCHARGE_CURRENT);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this._setMinCellVoltage(minimalCellVoltage);
	}

	public void setMinimalCellVoltageToUndefined() {
		this._setMinCellVoltage(null);
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this._setMaxCellVoltage(maximalCellVoltage);
	}

	public void setMaximalCellVoltageToUndefined() {
		this._setMaxCellVoltage(null);
	}

	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this._setMinCellTemperature(minimalCellTemperature);
	}

	public void setMinimalCellTemperatureToUndefined() {
		this._setMinCellTemperature(null);
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this._setMaxCellTemperature(maximalCellTemperature);
	}

	public void setMaximalCellTemperatureToUndefined() {
		this._setMaxCellTemperature(null);
	}

	public void setSoc(int soc) {
		this._setSoc(soc);
	}

	public void setSocToUndefined() {
		this._setSoc(null);
	}

	public void setMaximalChargeCurrent(int max) {
		this._setChargeMaxCurrent(max);
	}

	public void setMaximalChargeCurrentToUndefined() {
		this._setChargeMaxCurrent(null);
	}

	public void setMaximalDischargeCurrent(int max) {
		this._setDischargeMaxCurrent(max);
	}

	public void setMaximalDischargeCurrentToUndefined() {
		this._setDischargeMaxCurrent(null);
	}

	public void setVoltage(int voltage) {
		this._setVoltage(voltage);
	}

	public void setVoltageToUndefined() {
		this._setVoltage(null);
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isStopped() {
		return !running;
	}

	public float getMinimalCellVoltage() {
		return getMinCellVoltage().orElse(0);
	}

	public float getMaximalCellVoltage() {
		return getMaxCellVoltage().orElse(0);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}
}
