package io.openems.edge.controller.channelthreshold;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first start.
	 */
	UNDEFINED(-1, "Undefined"), //
	/**
	 * Value is smaller than the low threshold.
	 */
	BELOW_LOW(0, "Below Low"),
	/**
	 * Value just passed the low threshold. Last value was even lower.
	 */
	PASS_LOW_COMING_FROM_BELOW(1, "Pass Low Coming From Below"),
	/**
	 * Value just passed the low threshold. Last value was higher.
	 */
	PASS_LOW_COMING_FROM_ABOVE(2, "Pass Low Coming From Above"),
	/**
	 * Value is between low and high threshold.
	 */
	BETWEEN_LOW_AND_HIGH(3, "Between Low And High"),
	/**
	 * Value just passed the high threshold. Last value was lower.
	 */
	PASS_HIGH_COMING_FROM_BELOW(4, "Pass High Coming From Below"),
	/**
	 * Value just passed the high threshold. Last value was higher.
	 */
	PASS_HIGH_COMING_FROM_ABOVE(5, "Pass High Coming From Above"),
	/**
	 * Value is bigger than the high threshold.
	 */
	ABOVE_HIGH(6, "Above High");

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