package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum StatusDigitalInput implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "OFF"), //
	ON(308, "ON");

	private final int value;
	private final String name;

	private StatusDigitalInput(int value, String name) {
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