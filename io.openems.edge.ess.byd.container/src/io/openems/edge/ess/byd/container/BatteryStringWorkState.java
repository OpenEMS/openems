package io.openems.edge.ess.byd.container;

import io.openems.common.types.OptionsEnum;

public enum BatteryStringWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(0, "Initial"), //
	FAULT(2, "Fault"), //
	STARTING(4, "Starting"), //
	RUNNING(8, "Running"), //
	FAULTS(16, "Faults"),;

	private final int value;
	private final String name;

	private BatteryStringWorkState(int value, String name) {
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
