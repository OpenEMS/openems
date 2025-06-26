package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EmsCheck implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHECKING(0, "Checking"), //
	NORMAL(1, "Normal"), //
	FAULT(2, "Fault");

	private final int value;
	private final String option;

	private EmsCheck(int value, String option) {
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