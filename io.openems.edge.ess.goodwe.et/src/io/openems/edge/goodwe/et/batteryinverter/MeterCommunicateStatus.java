package io.openems.edge.goodwe.et.batteryinverter;

import io.openems.common.types.OptionsEnum;

enum MeterCommunicateStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NG(0, "NG"), //
	OK(1, "OK"); //

	private int value;
	private String option;

	private MeterCommunicateStatus(int value, String option) {
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