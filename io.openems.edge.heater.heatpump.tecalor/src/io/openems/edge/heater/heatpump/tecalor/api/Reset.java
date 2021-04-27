package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.types.OptionsEnum;

public enum Reset implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	RESETSYSTEM(1, "Rset System"), //
	RESETERROR(2, "Reset Fehlerliste"), //
	RESETHEATPUMP(3, "Reset Wärmepumpe"); //


	private int value;
	private String name;

	private Reset(int value, String name) {
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