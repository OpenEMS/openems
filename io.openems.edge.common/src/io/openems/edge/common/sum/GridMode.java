package io.openems.edge.common.sum;

import io.openems.common.types.OptionsEnum;

public enum GridMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON_GRID(1, "On-Grid"), //
	OFF_GRID(2, "Off-Grid");

	private int value;
	private String name;

	private GridMode(int value, String name) {
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