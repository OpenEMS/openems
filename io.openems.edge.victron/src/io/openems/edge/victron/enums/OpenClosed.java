package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum OpenClosed implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OPEN(0, "Open"), //
	CLOSED(1, "Closed");

	private final int value;
	private final String name;

	private OpenClosed(int value, String name) {
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