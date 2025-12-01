package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum AllowDisallow implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISALLOWED(0, "Disallowed"), //
	ALLOWED(1, "Allowed");

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