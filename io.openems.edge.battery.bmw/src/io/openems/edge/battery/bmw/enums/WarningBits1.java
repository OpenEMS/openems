package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum WarningBits1 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNSPECIFIED(0, "Unspecified Warning"), //
	LOW_VOLTAGE(1, "Low Voltage Warning"), //
	HIGH_VOLTAGE(2, "High Voltage Warning"), //
	CHARGE_CURRENT(3, "Charge Current Warning"), //
	DISCHARGE_CURRENT(4, "Discharge Current Warning"), //
	CHARGE_POWER(5, "Charge Power Warning"), //
	DISCHARGE_POWER(6, "Discharge Power Warning"), //
	LOW_SOC(7, "Low SoC Warning"), //
	HIGH_SOC(8, "High SoC Warning"), //
	LOW_TEMPERATURE(9, "Low Temperature Warning"), //
	HIGH_TEMPERATURE(10, "High Temperature Warning"), //
	INSULATION(11, "Insulation Warning"), //
	CONTACTOR_FUSE(12, "Contactor/Fuse Warning"), //
	SENSOR(13, "Sensor Warning"), //
	IMBALANCE(14, "Imbalance Warning"), //
	COMMUNICATION(15, "Communication Warning");

	private final int value;
	private final String name;

	private WarningBits1(int value, String name) {
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
