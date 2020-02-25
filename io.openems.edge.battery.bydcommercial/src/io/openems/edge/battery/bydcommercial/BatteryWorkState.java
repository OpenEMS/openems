package io.openems.edge.battery.bydcommercial;

import io.openems.common.types.OptionsEnum;

public enum BatteryWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDBY(0, "Standby"), //
	DISCHARGE(1, "Discharge"), //
	CHARGE(2, "Charge");

	int value;
	String name;

	private BatteryWorkState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}