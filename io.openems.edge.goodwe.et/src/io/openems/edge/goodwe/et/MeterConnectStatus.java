package io.openems.edge.goodwe.et;

import io.openems.common.types.OptionsEnum;

enum MeterConnectStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_CHECKED(0, "Not Checked"), //
	CORRECT(1, "connect correctly"), //
	REVERSE(2, "connect reverse"), //
	INCORRRECT(3, "connect incorrectly"), //
	FAULT(4, "Fault,fault mode,something is in fault mode"); //

	private int value;
	private String option;

	private MeterConnectStatus(int value, String option) {
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