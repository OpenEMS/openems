package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	LOCAL(0, "Local"), //
	REMOTE(1, "Remote"); //

	
	private int value;
	private String name;

	private ControlMode(int value, String name) {
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
