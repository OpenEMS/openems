package io.openems.edge.ess.mr.gridcon.helper;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;

public class DummyBattery extends AbstractOpenemsComponent implements Battery, SoltaroBattery {

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
		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this.getMinCellVoltage().setNextValue(null);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this.getMaxCellVoltage().setNextValue(maximalCellVoltage);
		this.getMaxCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltageToUndefined() {
		this.getMaxCellVoltage().setNextValue(null);
		this.getMaxCellVoltage().nextProcessImage();
	}

	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this.getMinCellTemperature().setNextValue(minimalCellTemperature);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMinimalCellTemperatureToUndefined() {
		this.getMinCellTemperature().setNextValue(null);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this.getMaxCellTemperature().setNextValue(maximalCellTemperature);
		this.getMaxCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperatureToUndefined() {
		this.getMaxCellTemperature().setNextValue(null);
		this.getMaxCellTemperature().nextProcessImage();
	}

	public void setSoc(int soc) {
		this.getSoc().setNextValue(soc);
		this.getSoc().nextProcessImage();
	}

	public void setSocToUndefined() {
		this.getSoc().setNextValue(null);
		this.getSoc().nextProcessImage();
	}
	
	public void setMaximalChargeCurrent(int max) {
		this.getChargeMaxCurrent().setNextValue(max);
		this.getChargeMaxCurrent().nextProcessImage();
	}

	public void setMaximalChargeCurrentToUndefined() {
		this.getChargeMaxCurrent().setNextValue(null);
		this.getChargeMaxCurrent().nextProcessImage();
	}
	
	public void setMaximalDischargeCurrent(int max) {
		this.getDischargeMaxCurrent().setNextValue(max);
		this.getDischargeMaxCurrent().nextProcessImage();
	}

	public void setMaximalDischargeCurrentToUndefined() {
		this.getDischargeMaxCurrent().setNextValue(null);
		this.getDischargeMaxCurrent().nextProcessImage();
	}
	
	public void setVoltage(int voltage) {
		this.getVoltage().setNextValue(voltage);
		this.getVoltage().nextProcessImage();
	}

	public void setVoltageToUndefined() {
		this.getVoltage().setNextValue(null);
		this.getVoltage().nextProcessImage();
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
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean isStopped() {
		return !running;
	}

	@Override
	public boolean isError() {
		return error;
	}

	@Override
	public float getMinimalCellVoltage() {
		return getMinCellVoltage().value().orElse(0);
	}

	@Override
	public float getMaximalCellVoltage() {
		return getMaxCellVoltage().value().orElse(0);
	}

	@Override
	public float getSoCX() {
		return getSoc().value().orElse(0);
	}

	@Override
	public float getCapacityX() {
		return getCapacity().value().orElse(0);
	}

	@Override
	public float getCurrentX() {
		return getCurrent().value().orElse(0);
	}

	@Override
	public float getVoltageX() {
		return getVoltage().value().orElse(0);
	}

	@Override
	public float getMaxChargeCurrentX() {
		return getChargeMaxCurrent().value().orElse(0);
	}

	@Override
	public float getMaxDischargeCurrentX() {
		return getDischargeMaxCurrent().value().orElse(0);
	}

	@Override
	public float getMaxChargeVoltageX() {
		return getChargeMaxVoltage().value().orElse(0);
	}

	@Override
	public float getMinDischargeVoltageX() {
		return getDischargeMinVoltage().value().orElse(0);
	}
}
