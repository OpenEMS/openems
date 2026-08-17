package io.openems.edge.fronius.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(1, "Off"), //
	EMPTY(2, "Empty"), //
	DISCHARGING(3, "Discharging"), //
	CHARGING(4, "Charging"), //
	FULL(5, "Full"), //
	STANDBY(6, "Standby / Holding"), //
	TESTING(7, "Test mode");

	private final int value;
	private final String name;

	BatteryState(int value, String name) {
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