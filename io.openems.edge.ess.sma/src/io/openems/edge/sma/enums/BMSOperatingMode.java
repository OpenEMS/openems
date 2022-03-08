package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum BMSOperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	ON(308, "On"), //
	BATTERY_CHARGING(2289, "Battery Charging"), //
	BATTERY_DISCHARGING(2290, "Battery Discharging"), //
	DEFAULT_SETTING(2424, "Default Setting");

	private final int value;
	private final String name;

	private BMSOperatingMode(int value, String name) {
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