package io.openems.edge.deye.common;

import io.openems.common.types.OptionsEnum;

public enum SystemManufacturer implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BYD(1, "BYD");

	private final int value;
	private final String name;

	private SystemManufacturer(int value, String name) {
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