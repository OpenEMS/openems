package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum AppModeIndex implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SELF_USE(0, "Self use mode"), //
	OFF_GRID(1, "Off grid mode"), //
	BACKUP(2, "backup mode"), //
	ECONOMIC(3, "economic mode"); //

	private final int value;
	private final String option;

	private AppModeIndex(int value, String option) {
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