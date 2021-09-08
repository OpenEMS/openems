package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EnableCurve implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Feed Power Disable"), //
	ENABLE(1, "Feed Power Enable");//

	private final int value;
	private final String option;

	private EnableCurve(int value, String option) {
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