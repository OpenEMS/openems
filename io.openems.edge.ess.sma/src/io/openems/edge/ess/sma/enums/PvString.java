package io.openems.edge.ess.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum PvString implements OptionsEnum {
	ONE(1, "String 1"), //
	TWO(2, "String 2");

	private final int value;
	private final String name;

	PvString(int value, String name) {
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
		return ONE;
	}
}
