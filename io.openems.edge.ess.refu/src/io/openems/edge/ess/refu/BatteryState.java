package io.openems.edge.ess.refu;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(0, "Initial"), //
	STOP(1, "STOP"), //
	STARTING(2, "Starting"), //
	START(3, "START"), //
	STOPPING(4, "Stopping"), //
	FAULT(5, "Fault");

	private final int value;
	private final String option;

	private BatteryState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}