package io.openems.edge.deye.common;

import io.openems.common.types.OptionsEnum;

public enum BmsDcdcWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(2, "Initial"), //
	STOP(4, "Stop"), //
	READY(8, "Ready"), //
	RUNNING(16, "Running"), //
	FAULT(32, "Fault"), //
	DEBUG(64, "Debug"), //
	LOCKED(128, "Locked"); //

	private final int value;
	private final String name;

	private BmsDcdcWorkState(int value, String name) {
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