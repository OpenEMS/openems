package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum ArcSelfCheckStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	FAILURE(0, "Failure"), //
	ACTIVATED(1, "Activated");

	private final int value;
	private final String option;

	private ArcSelfCheckStatus(int value, String option) {
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