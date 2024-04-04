package io.openems.edge.deye.common;

import io.openems.common.types.OptionsEnum;

public enum SetWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(4, "Stop"), //
	STANDBY(32, "Standby"), //
	START(64, "Start"); //

	private final int value;
	private final String name;

	private SetWorkState(int value, String name) {
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