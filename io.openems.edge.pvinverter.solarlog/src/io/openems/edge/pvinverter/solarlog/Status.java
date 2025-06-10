package io.openems.edge.pvinverter.solarlog;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OK(0, "Ok"), //
	LICENSE_NOT_SUFFICIENT(1, "License not sufficient for size of installation"); //

	private final int value;
	private final String name;

	private Status(int value, String name) {
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