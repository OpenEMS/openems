package io.openems.edge.common.sum;

import io.openems.edge.common.channel.OptionsEnum;

public enum GridMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON_GRID(0, "On-Grid"), //
	OFF_GRID(1, "Off-Grid");

	private int value;
	private String name;

	private GridMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}