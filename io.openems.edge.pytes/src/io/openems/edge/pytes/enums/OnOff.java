package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum OnOff implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	OFF(0, "OFF"),
	ON(1, "ON");

	private final int value;
	private final String name;

	OnOff(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
