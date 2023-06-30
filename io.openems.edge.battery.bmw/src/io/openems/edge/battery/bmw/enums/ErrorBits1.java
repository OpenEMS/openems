package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum ErrorBits1 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNSPECIFIED(0, "Unspecified Error"), //
	LOW_VOLTAGE(1, "Low Voltage Error"), //
	HIGH_VOLTAGE(2, "High Voltage Error"), //
	CHARGE_CURRENT(3, "Charge Current Error"), //
	DISCHARGE_CURRENT(4, "Discharge Current Error"), //
	CHARGE_POWER(5, "Charge Power Error"), //
	DISCHARGE_POWER(6, "Discharge Power Error"), //
	LOW_SOC(7, "Low SoC Error"), //
	HIGH_SOC(8, "High SoC Error"), //
	LOW_TEMPERATURE(9, "Low Temperature Error"), //
	HIGH_TEMPERATURE(10, "High Temperature Error"), //
	INSULATION(11, "Insulation Error"), //
	CONTACTOR_FUSE(12, "Contactor/Fuse Error"), //
	SENSOR(13, "Sensor Error"), //
	IMBALANCE(14, "Imbalance Error"), //
	COMMUNICATION(15, "Communication Error");

	private final int value;
	private final String name;

	private ErrorBits1(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
