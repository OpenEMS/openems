package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum BatteryWorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_BATTERY(0, "NO Battery,inverter disconnects to Battery"), //
	STANDBY(1, "Standby,no diacharging and no charging"), //
	DISCHARGING(2, "Discharging"), //
	CHARGING(3, "Charging"), //
	WAITING_FOR_CHARGE(4, "Waiting for charge"), //
	WAITING_FOR_DISCHARGE(5, "Waiting for discharge");

	private int value;
	private String option;

	private BatteryWorkMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}	
}