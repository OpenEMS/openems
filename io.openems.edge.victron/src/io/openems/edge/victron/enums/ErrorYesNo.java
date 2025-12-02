package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum ErrorYesNo implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ERROR(0, "No Error"), //
	FAULT(1, "Error");

	private final int value;
	private final String name;

	private ErrorYesNo(int value, String name) {
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