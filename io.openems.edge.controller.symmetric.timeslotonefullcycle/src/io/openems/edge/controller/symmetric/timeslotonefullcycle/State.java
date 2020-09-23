package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	FINISHED(0, "Finished"), //
	FIRST_CHARGE(1, "First Charge"), //
	FIRST_DISCHARGE(2, "First Discharge"), //
	SECOND_CHARGE(3, "Second Charge"), //
	SECOND_DISCHARGE(4, "Second Discharge"); //

	private final int value;
	private final String name;

	private State(int value, String name) {
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