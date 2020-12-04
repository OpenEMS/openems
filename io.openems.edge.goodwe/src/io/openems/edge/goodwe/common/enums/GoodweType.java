package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GoodweType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GOODWE_10K_BT(10, "GoodWe GW10K-BT"), //
	GOODWE_8K_BT(11, "GoodWe GW8K-BT"), //
	GOODWE_5K_BT(12, "GoodWe GW5K-BT"), //
	GOODWE_10K_ET(20, "GoodWe GW10K-ET"), //
	GOODWE_8K_ET(21, "GoodWe GW8K-ET"), //
	GOODWE_5K_ET(22, "GoodWe GW5K-ET");

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