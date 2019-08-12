package io.openems.edge.goodwe.et.batteryinverter;

import io.openems.common.types.OptionsEnum;

enum LoadMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON(0, "ON,inverter connects to Load"), //
	OFF(1, "OFF, inverter disconnects to Load"); //
	
	private int value;
	private String option;

	private LoadMode(int value, String option) {
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