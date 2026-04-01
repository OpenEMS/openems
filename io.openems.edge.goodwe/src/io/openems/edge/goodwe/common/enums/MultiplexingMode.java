package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum MultiplexingMode implements OptionsEnum {

	UNDEFINED(-1, "UNDEFINED"), //
	GENSET(0, "GENSET"), //
	LARGE_LOAD(1, "LARGE_LOAD"), //
	BACKUP(2, "BACKUP");

	private final int value;
	private final String name;

	MultiplexingMode(int value, String name) {
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
