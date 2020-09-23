package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import io.openems.common.types.OptionsEnum;

public enum CycleOrder implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	START_WITH_CHARGE(0, "Start with Charge"), //
	START_WITH_DISCHARGE(1, "Start with Discharge");

	private final int value;
	private final String name;

	private CycleOrder(int value, String name) {
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