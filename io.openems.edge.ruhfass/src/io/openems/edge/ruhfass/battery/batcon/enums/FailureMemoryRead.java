package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum FailureMemoryRead implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_READ(0, "No Read"), //
	READ(1, "Read"); //

	private int value;
	private String name;

	private FailureMemoryRead(int value, String name) {
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
