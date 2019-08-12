package io.openems.edge.goodwe.et.batteryinverter;

import io.openems.common.types.OptionsEnum;

enum OperationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT(0x01, "cut off all the connection to Inverter"), //
	ONLINE(0x02, "PV intputs to Inverter,Inverter outputs to Grid"), //
	BATTERY(0x04, "PV inputs to Inverter(First),Battery inputs to Inverter(Second),Inverter work as AC source"), //
	FAULT(0x10, "Fault,fault mode,something is in fault mode"); //
	
	private int value;
	private String option;

	private OperationMode(int value, String option) {
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