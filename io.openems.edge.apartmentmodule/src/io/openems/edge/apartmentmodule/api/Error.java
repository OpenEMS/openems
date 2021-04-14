package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

public enum Error implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ERROR(0, "No Error"), //
	ERROR_BIT_001(1, "Modbus communication error. No signal from master received for 1 minute."), //
	ERROR_BIT_010(2, "Relay activation command timeout."), //
	ERROR_BIT_011(3, "Relay activation command timeout and Modbus communication error."), //
	ERROR_BIT_100(4, "Temperature sensor error."), //
	ERROR_BIT_101(5, "Temperature sensor error and Modbus communication error."), //
	ERROR_BIT_110(6, "Temperature sensor error and Relay activation command timeout."), //
	ERROR_BIT_111(7, "Temperature sensor error, Relay activation command timeout and Modbus communication error."); //

	private int value;
	private String name;

	private Error(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}	
}