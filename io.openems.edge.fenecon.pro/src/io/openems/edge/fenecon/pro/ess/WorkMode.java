package io.openems.edge.fenecon.pro.ess;

import io.openems.common.types.OptionsEnum;

public enum WorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ECONOMOY(2, "Economy"), //
	REMOTE(6, "Remote"), //
	TIMING(8, "Timing");

	private final int value;
	private final String name;

	private WorkMode(int value, String name) {
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