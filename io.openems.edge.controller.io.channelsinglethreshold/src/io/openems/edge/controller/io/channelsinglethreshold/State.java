package io.openems.edge.controller.io.channelsinglethreshold;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first start.
	 */
	UNDEFINED(-1, "Undefined"), //
	/**
	 * Value is smaller than the low threshold.
	 */
	BELOW_THRESHOLD(0, "Below Threshold"),
	/**
	 * Value is bigger than the high threshold.
	 */
	ABOVE_THRESHOLD(1, "Above Threshold");

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