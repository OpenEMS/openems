package io.openems.edge.goodwe.et.ess;

import io.openems.common.types.OptionsEnum;

public enum GridMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF_GRID(0, "Loss, inverter disconnects to Grid"), //
	ON_GRID(1, "OK, inverter connects to Grid"), //
	FAULT(2, "Fault,something is wrong"); //

	private final int value;
	private final String option;

	private GridMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}