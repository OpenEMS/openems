package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GridWaveCheckLevel implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	HIGH_SENSITIVTY(0, "MPPT shadow scan enable"), //
	LOW_SENSITIVITY(1, "MPPT shadow scan disable"), //
	CLOSE(2, "MPPT shadow scan disable")//
	;//

	private final int value;
	private final String option;

	private GridWaveCheckLevel(int value, String option) {
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