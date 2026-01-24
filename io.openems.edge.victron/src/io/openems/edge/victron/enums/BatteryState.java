package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	IDLE(0, "Idle"), //
	CHARGING(1, "Charging"), //
	DISCHARGING(2, "Discharging");

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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
