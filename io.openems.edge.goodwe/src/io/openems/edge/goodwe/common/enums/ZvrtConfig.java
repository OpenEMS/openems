package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum ZvrtConfig implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Disable"), //
	ONLY_LVRT_ENABLE(1, "Only LVRT enable"), //
	ONLY_HVRT_ENABLE(1, "Only LVRT enable"), //
	BOTH_ENABLE(1, "Only LVRT enable");//

	private final int value;
	private final String option;

	private ZvrtConfig(int value, String option) {
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