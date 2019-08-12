package io.openems.edge.goodwe.et.batteryinverter;

import io.openems.common.types.OptionsEnum;

enum GridMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF_GRID(0, "Loss, inverter disconnects to Grid"), //
	ON_GRID(1, "OK, inverter connects to Grid"), //
	FAULT(2, "Fault,something is wrong"); //
	
	
	
	private int value;
	private String option;

	private GridMode(int value, String option) {
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