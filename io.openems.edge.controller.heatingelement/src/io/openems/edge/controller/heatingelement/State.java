package io.openems.edge.controller.heatingelement;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first state.
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * When production is 2000W more than the capacity of the ESS, first phase was switched on.
	 */
	FIRST_PHASE(0, "SWITCH ON first phase"),
	/**
	 * When production is 4000W more than the capacity of the ESS, second phase was switched on.
	 */
	SECOND_PHASE(1, "SWITCH ON second phase"),
	/**
	 * When production is 4000W more than the capacity of the ESS, third phase was switched on.
	 */
	THIRD_PHASE(2, "SWITCH ON third phase");
	

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