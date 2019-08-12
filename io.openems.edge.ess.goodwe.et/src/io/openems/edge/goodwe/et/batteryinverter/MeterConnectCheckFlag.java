package io.openems.edge.goodwe.et.batteryinverter;

import io.openems.common.types.OptionsEnum;

enum MeterConnectCheckFlag implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(0, "Stop Checking"), //
	CHECKING(1, "Checking"), //
	WAIT(2, "Wait for check"); //
	
	private int value;
	private String option;

	private MeterConnectCheckFlag(int value, String option) {
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