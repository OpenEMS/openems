package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterReverseEnable implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	REVERSE_DISABLE(0, "Smart meter ct1 reverse disable"), //
	REVERSE_ENABLE(1, "Smart meter ct1 reverse enable");

	private final int value;
	private final String option;

	private MeterReverseEnable(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}