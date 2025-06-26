package io.openems.edge.fenecon.mini.ess;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDBY(0, "Standby"), //
	START_OFF_GRID(1, "Start Off-Grid"), //
	START(2, "START"), //
	FAULT(3, "FAULT"), //
	OFF_GRID_PV(4, "Off-Grid PV");

	private final int value;
	private final String name;

	private SystemState(int value, String name) {
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