package io.openems.edge.common.offgrid;

import io.openems.common.types.OptionsEnum;

public enum OperationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ONLY_ON_GRID(0, "Operate only in On Grid"), //
	ON_AND_OFF_GRID(1, "Operate as off-gridable");

	private int value;
	private String name;

	private OperationMode(int value, String name) {
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