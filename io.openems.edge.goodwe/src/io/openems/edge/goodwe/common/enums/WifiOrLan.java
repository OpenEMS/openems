package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum WifiOrLan implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WIFI(4, "Led On"), //
	LAN(5, "Led flash")//
	;//

	private final int value;
	private final String option;

	private WifiOrLan(int value, String option) {
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