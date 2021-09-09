package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EzloggerProCommStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NG(0x00, "Not good "), //
	SUCCESS(0x01, " Success");

	private final int value;
	private final String option;

	private EzloggerProCommStatus(int value, String option) {
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