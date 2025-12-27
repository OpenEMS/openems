package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum Position implements OptionsEnum {
	UNDEFINED(-1, "undefined"), //
	AC_INPUT_1(0, "AC input 1"), //
	AC_OUTPUT(1, "AC output"), //
	AC_INPUT_2(2, "AC input 2");

	private final int value;
	private final String option;

	private Position(int value, String option) {
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
