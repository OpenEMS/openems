package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterConnectStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CORRECT(1, "connect correctly"), //
	REVERSE(2, "connect reverse"), //
	INCORRRECT(3, "connect incorrectly");

	private final int value;
	private final String option;

	private MeterConnectStatus(int value, String option) {
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