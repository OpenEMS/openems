package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.common.types.OptionsEnum;

enum ChargeState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISCHARGE(0, "Discharge"), //
	CHARGE(1, "Charge");

	private final int value;
	private final String name;

	private ChargeState(int value, String name) {
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