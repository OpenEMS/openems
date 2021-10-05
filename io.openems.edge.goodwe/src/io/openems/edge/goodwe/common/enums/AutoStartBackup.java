package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum AutoStartBackup implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off grid auto startup is off"), //
	ON(1, "Off grid auto startup is on");//

	private final int value;
	private final String option;

	private AutoStartBackup(int value, String option) {
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