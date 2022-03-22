package io.openems.edge.controller.io.heatingelement.enums;

import io.openems.common.types.OptionsEnum;

public enum Level implements OptionsEnum {
	/**
	 * Unknown state on first state.
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * Unknown state on first state.
	 */
	LEVEL_0(0, "Switch off everything"),
	/**
	 * When grid-feed-in is more than 2000 W, first phase is switched on.
	 */
	LEVEL_1(1, "Switch-on only first phase"),
	/**
	 * When grid-feed-in is more than 4000 W, first and second phase are switched
	 * on.
	 */
	LEVEL_2(2, "Switch-on first and second phase"),
	/**
	 * When grid-feed-in is more than 4000 W, all phases are switched on.
	 */
	LEVEL_3(3, "Switch-on all the three phase");

	private final int value;
	private final String name;

	private Level(int value, String name) {
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
		return LEVEL_0;
	}
}