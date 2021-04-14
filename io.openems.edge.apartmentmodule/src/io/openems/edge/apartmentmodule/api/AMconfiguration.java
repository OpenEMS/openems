package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

public enum AMconfiguration implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BOTTOM(0, "Bottom"), //
	TOP(1, "Top"); //

	private int value;
	private String name;

	private AMconfiguration(int value, String name) {
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