package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum OnOff implements OptionsEnum {
	UNDEFINED(-1, "undefined"), //
	ON(1, "On"), //
	OFF(4, "Off");

	private final int value;
	private final String name;

	private OnOff(int value, String name) {
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
