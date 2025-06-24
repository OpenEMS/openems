package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;

public enum WorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal operation"), //	
	INITIALIZING(1, "Initializing"), //
	ERROR(2, "Fault state"), //
	WARNING(3, "Warning state"), //	
	STOP(4, "Stop"), //
	STANDBY(32, "Standby"), //
	START(64, "Start"); //

	private final int value;
	private final String name;

	private WorkState(int value, String name) {
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
		return UNDEFINED;
	}
}