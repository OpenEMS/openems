package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum WorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT(0, "cut off all the connection to Inverter"), //
	ON_GRID(1, "PV inputs to Inverter,Inverter outputs to Grid"), //
	OFF_GRID(2, "PV inputs to Inverter(First),Battery inputs to Inverter(Second),Inverter work as AC source"), //
	FAULT(3, "Fault,fault mode,something is in fault mode"), //
	FLASH(4, "Inverter upgrade"), //
	CHECK(5, "Power on self-check of inverter");

	private final int value;
	private final String option;

	private WorkMode(int value, String option) {
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