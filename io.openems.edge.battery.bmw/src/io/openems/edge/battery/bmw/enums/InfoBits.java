package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum InfoBits implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	
	;

	private final int value;
	private final String name;

	private InfoBits(int value, String name) {
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
