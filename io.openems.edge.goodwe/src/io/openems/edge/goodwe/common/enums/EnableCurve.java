package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EnableCurve implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Disable Curve"), //
	ENABLE(1, "Enable Curve"); //

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