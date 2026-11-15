package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum PvConnectMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	EACH_MPPT(0, "For each MPPT"), //
	TWO_MPPT(1, "For 12, 34 MPPT"), //
	ONE_MPPT(2, "For 1 MPPT"), //
	;

	private final int value;
	private final String option;

	private PvConnectMode(int value, String option) {
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