package io.openems.edge.ess.fenecon.commercial40;

import io.openems.common.types.OptionsEnum;

public enum SystemType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CESS(1, "CESS"); //

	private final int value;
	private final String name;

	private SystemType(int value, String name) {
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