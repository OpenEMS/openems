package io.openems.edge.kostal.piko.core.api;

import io.openems.common.types.OptionsEnum;

public enum BatteryCurrentDirection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE(0, "Charge"), //
	DISCHARGE(1, "Discharge");

	private final int value;
	private final String name;

	private BatteryCurrentDirection(int value, String name) {
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