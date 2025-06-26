package io.openems.edge.fenecon.pro.ess;

import io.openems.common.types.OptionsEnum;

public enum SetWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	LOCAL_CONTROL(0, "Local Control"), //
	START(1, "Start"), //
	REMOTE_CONTROL_OFF_GRID_STARTING(2, "Remote Control off Grid Starting"), //
	STOP(3, "Stop"), //
	EMERGENCY_STOP(4, "Emergency Stop"); //

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