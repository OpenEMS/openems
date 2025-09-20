package io.openems.edge.controller.chp.soc;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first start or Value is larger than the low threshold and
	 * smaller than the high threshold.
	 */
	UNDEFINED(-1, "Undefined"),
	/**
	 * Value is smaller than the low threshold.
	 */
	ON(0, "Digital output ON"),
	/**
	 * Value is larger than the high threshold.
	 */
	OFF(1, "Digital output OFF");

	private final int value;
	private final String name;

	private State(int value, String name) {
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