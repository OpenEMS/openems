package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum OperationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT(0x01, "cut off all the connection to Inverter"), //
	ONLINE(0x02, "PV inputs to Inverter,Inverter outputs to Grid"), //
	BATTERY(0x04, "PV inputs to Inverter(First),Battery inputs to Inverter(Second),Inverter work as AC source"), //
	FAULT(0x10, "Fault,fault mode,something is in fault mode"); //

	private final int value;
	private final String option;

	private OperationMode(int value, String option) {
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