package io.openems.common.test;

import io.openems.common.types.OptionsEnum;

public enum DummyOptionsEnum implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	VALUE_1(1, "One"), //
	;

	private final int value;
	private final String name;

	private DummyOptionsEnum(int value, String name) {
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