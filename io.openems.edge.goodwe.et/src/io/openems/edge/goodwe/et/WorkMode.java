package io.openems.edge.goodwe.et;

import io.openems.common.types.OptionsEnum;

enum WorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT(0, "cut off all the connection to Inverter"), //
	ON_GRID(1, "PV intputs to Inverter,Inverter outputs to Grid"), //
	OFF_GRID(2, "PV inputs to Inverter(First),Battery inputs to Inverter(Second),Inverter work as AC source"), //
	FAULT(3, "Fault,fault mode,something is in fault mode"), //
;

	private int value;
	private String option;

	private WorkMode(int value, String option) {
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