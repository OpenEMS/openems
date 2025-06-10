package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum LedState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(1, "Led Off"), //
	ON(2, "Led On"), //
	FLASH_1X(3, "Led flash"), //
	FLASH_2X(4, "Led flash"), //
	FLASH_4X(5, "Led flash")//
	;//

	private final int value;
	private final String option;

	private LedState(int value, String option) {
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