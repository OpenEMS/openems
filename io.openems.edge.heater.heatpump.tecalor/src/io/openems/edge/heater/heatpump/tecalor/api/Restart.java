package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.types.OptionsEnum;

public enum Restart implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Aus"), //
	RESTART(1, "Restart"), //
	SERVICEBUTTON(2, "Service Taste"); //



	private int value;
	private String name;

	private Restart(int value, String name) {
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