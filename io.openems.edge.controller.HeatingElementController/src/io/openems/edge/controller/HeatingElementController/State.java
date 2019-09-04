package io.openems.edge.controller.HeatingElementController;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first state.
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * When production is 2000W more than the capacity of the ESS.
	 */
	FIRST_PHASE(0, "Digital output On/Off first relay"),
	/**
	 * Value Production is 2000W more than the First State.
	 */
	SECOND_PHASE(1, "Digital output On/Off second relay"),
	/**
	 * Value Production is 2000W more than the Second State.
	 */
	THIRD_PHASE(2, "Digital output On/Off third relay");

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