package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum LoadRegulationIndex implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, " Disable"), //
	SWITCHING_MODE(1, "Switching mode"), //
	TIME_MANAGE(2, "Time manage"),//
	;

	private final int value;
	private final String option;

	private LoadRegulationIndex(int value, String option) {
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