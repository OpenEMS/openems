package io.openems.edge.evcs.spelsberg.smart;

import io.openems.common.types.OptionsEnum;

public enum EvseState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	STARTING(0, "Starting"), //
	RUNNING(1, "Running"), //
	ERROR(2, "Error"), //
	;

	private final int value;
	private final String name;

	private EvseState(int value, String name) {
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