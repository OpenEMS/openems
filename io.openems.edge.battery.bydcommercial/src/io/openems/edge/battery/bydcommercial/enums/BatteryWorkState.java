package io.openems.edge.battery.bydcommercial.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDBY(0, "Standby"), //
	DISCHARGE(1, "Discharge"), //
	CHARGE(2, "Charge");

	private final int value;
	private final String name;

	private BatteryWorkState(int value, String name) {
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