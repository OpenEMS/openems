package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum InfoBits implements OptionsEnum {
	UNDEFINED(-1, "Undefined");

	private final int value;
	private final String name;

	private InfoBits(int value, String name) {
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
