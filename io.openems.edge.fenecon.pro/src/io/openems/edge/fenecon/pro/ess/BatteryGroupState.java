package io.openems.edge.fenecon.pro.ess;

import io.openems.common.types.OptionsEnum;

public enum BatteryGroupState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(0, "Initial"), //
	STOP(1, "Stop"), //
	STARTING(2, "Starting"), //
	RUNNING(3, "Running"), //
	STOPPING(4, "Stopping"), //
	FAIL(5, "Fail");

	private final int value;
	private final String name;

	private BatteryGroupState(int value, String name) {
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