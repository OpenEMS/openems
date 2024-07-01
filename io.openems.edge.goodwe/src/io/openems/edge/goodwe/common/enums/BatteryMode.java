package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_BATTERY(0, "NO Battery,inverter disconnects to Battery"), //
	STANDBY(1, "Standby,no diacharging and no charging"), //
	DISCHARGING(2, "Discharging"), //
	CHARGING(3, "Charging");

	private final int value;
	private final String option;

	private BatteryMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}