package io.openems.edge.controller.heatingelement;

import io.openems.common.types.OptionsEnum;

public enum Level implements OptionsEnum {
	/**
	 * Unknown state on first state.
	 */
	LEVEL_0(-1, "Undefined"),
	/**
	 * When production is 2000W more than the capacity of the ESS, first phase was switched on.
	 */
	LEVEL_1(0, "Switch-on only first phase"),
	/**
	 * When production is 4000W more than the capacity of the ESS, second phase was switched on.
	 */
	LEVEL_2(1, "Switch-on first and second phase"),
	/**
	 * When production is 6000W more than the capacity of the ESS, third phase was switched on.
	 */
	LEVEL_3(2, "Switch-on all the three phase");
	

	private final int value;
	private final String name;

	private Level(int value, String name) {
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
		return LEVEL_0;
	}
}