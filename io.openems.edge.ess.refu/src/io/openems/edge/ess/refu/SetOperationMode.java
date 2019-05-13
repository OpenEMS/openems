package io.openems.edge.ess.refu;

import io.openems.common.types.OptionsEnum;

public enum SetOperationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PQ_SET_POINT(0, "P/Q Set point"), //
	IAC_COSPHI_SET_POINT(1, "IAC/cosphi set point");

	private final int value;
	private final String option;

	private SetOperationMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}