package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum AllowDisallow implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INVALID(0, "Invalid value"), //
	ALLOW(1, "Allow"), //
	DISALLOW(2, "Disallow") //
	;

	private final int value;
	private final String name;

	private AllowDisallow(int value, String name) {
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