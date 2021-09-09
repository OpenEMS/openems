package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum FeedPowerEnable implements OptionsEnum {

	// TODO Is 0 disables or enables, likewise 1 disables or enables

	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Feed Power Disable"), //
	ENABLE(1, "Feed Power Enable");//

	private final int value;
	private final String option;

	private FeedPowerEnable(int value, String option) {
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