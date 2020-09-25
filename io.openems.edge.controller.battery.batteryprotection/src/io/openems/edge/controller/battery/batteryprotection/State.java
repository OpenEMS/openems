package io.openems.edge.controller.battery.batteryprotection;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	LIMIT(1, "Limit"), //
	FORCE_CHARGE(3, "ForceCharge"), //
	FULL_CHARGE(4, "FullCharge"), //
	CHECK(5, "Check");

	private final int value;
	private final String name;

	private State(int value, String name) {
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