package io.openems.edge.ess.refubeckhoff.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	INIT(1, "Init"), //
	PRE_OPERATION(2, "Pre-operation"), //
	STANDBY(3, "Standby"), //
	OPERATION(4, "Operation"), //
	ERROR(5, "Error");

	private int value;
	private String option;

	private SystemState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}