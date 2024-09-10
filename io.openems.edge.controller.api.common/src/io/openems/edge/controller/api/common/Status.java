package io.openems.edge.controller.api.common;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {

	ACTIVE(0, "Active"), //

	INACTIVE(1, "Inactive"), //
	
	ERROR(2, "Error"); //


	private final int value;
	private final String name;

	private Status(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return INACTIVE;
	}
	
}
