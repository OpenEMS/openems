package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum InternalSocProtection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ENABLE(0, "Enable"), //
	DISABLE(1, "Disable");

	private final int value;
	private final String option;

	private InternalSocProtection(int value, String option) {
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