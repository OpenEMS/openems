package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryReset implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_BATTERY_RESET(0, "No Battery Reset"), //
	BATTERY_RESET(1, "Battery Reset"); //

	private int value;
	private String name;

	private BatteryReset(int value, String name) {
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
