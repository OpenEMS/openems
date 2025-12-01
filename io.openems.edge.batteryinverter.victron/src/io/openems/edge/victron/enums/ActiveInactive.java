package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum ActiveInactive implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INACTIVE(0, "Inactive"), //
	ACTIVE(1, "Active");

	private final int value;
	private final String name;

	private ActiveInactive(int value, String name) {
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