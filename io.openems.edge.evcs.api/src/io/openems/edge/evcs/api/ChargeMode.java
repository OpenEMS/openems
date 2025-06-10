package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum ChargeMode implements OptionsEnum {
	FORCE_CHARGE(0, "Force-Charge"), //
	EXCESS_POWER(1, "Use surplus power"); //

	private final int value;
	private final String name;

	private ChargeMode(int value, String name) {
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
		return FORCE_CHARGE;
	}
}