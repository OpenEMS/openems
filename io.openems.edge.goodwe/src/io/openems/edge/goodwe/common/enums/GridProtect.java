package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GridProtect implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GRID_PROTECT_ENABLE(0, "Enable grid protect"), //
	GRID_PROTECT_DISABLE(1, "Disable grid protect"), //
	;

	private final int value;
	private final String option;

	private GridProtect(int value, String option) {
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