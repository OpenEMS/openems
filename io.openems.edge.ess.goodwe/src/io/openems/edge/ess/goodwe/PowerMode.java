package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum PowerMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTO(1, "Battery power is automatically controlled by hybrid inverter, based on Meter Power"), //
	CHARGE(2, "Charge mode"), //
	DISCHARGE(3, "Charge mode"), //
	IMPORT(4, "Charge mode"), //
	EXPORT(5, "Power-selling Mode"), // 
	CONSERVE(6, "Charge mode"), // 
	OFFGRID(7, "Off-Grid Mode"), //
	PV_CHARGE(0x0A, "Charge mode"), //
	STOPPED(0xFF, " "); //
	
	private int value;
	private String option;

	private PowerMode(int value, String option) {
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