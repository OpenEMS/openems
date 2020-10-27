package io.openems.edge.goodwe.et.ess;

import io.openems.common.types.OptionsEnum;

public enum GoodweType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GOODWE_10K_BT(1, "GoodWe GW10K-BT"), //
	GOODWE_10K_ET(2, "GoodWe GW10K-ET");

	private final int value;
	private final String option;

	private GoodweType(int value, String option) {
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