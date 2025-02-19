package io.openems.edge.ess.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ERROR(35, "Error"), //
	OFF(303, "Off"), //
	OK(307, "OK"), //
	WARNING(455, "Warning");

	private final int value;
	private final String name;

	private SystemState(int value, String name) {
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