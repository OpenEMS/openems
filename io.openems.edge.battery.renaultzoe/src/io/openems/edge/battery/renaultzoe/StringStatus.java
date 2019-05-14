package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum StringStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Disable"), //
	ENABLE(1, "Enable"); //

	
	private int value;
	private String name;

	private StringStatus(int value, String name) {
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
