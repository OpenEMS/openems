package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum ExternalEmsFlag implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	ALPHA(10, "Alpha");

	private final int value;
	private final String option;

	private ExternalEmsFlag(int value, String option) {
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