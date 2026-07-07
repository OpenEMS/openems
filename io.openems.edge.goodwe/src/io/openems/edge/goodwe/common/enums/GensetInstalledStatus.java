package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GensetInstalledStatus implements OptionsEnum {

	UNDEFINED(-1, "UNDEFINED"), //
	INSTALLED(0, "INSTALLED"), //
	NOT_INSTALLED(2, "NOT_INSTALLED");

	private final int value;
	private final String name;

	GensetInstalledStatus(int value, String name) {
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
