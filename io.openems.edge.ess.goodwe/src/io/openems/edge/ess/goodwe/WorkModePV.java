package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum WorkModePV implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_PV(0, "NO PV,inverter disconnects to PV"), //
	STANDBY(1, "Standby,PV does not output power"), //
	WORKING(2, "Work, PV output power");

	private int value;
	private String option;

	private WorkModePV(int value, String option) {
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