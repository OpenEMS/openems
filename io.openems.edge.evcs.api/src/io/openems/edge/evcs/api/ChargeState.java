package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum ChargeState implements OptionsEnum {
	/**
	 * Undefined.
	 */
	UNDEFINED(-1, "Undefined"), //

	/**
	 * EVCS is not charging.
	 */
	NOT_CHARGING(0, "Not charging"), //

	/**
	 * EVCS is charging.
	 */
	CHARGING(1, "Charging"), //

	/**
	 * Decreasing the charging limit and waiting for EVCS-specific reaction time.
	 */
	DECREASING(2, "Decreasing"), //

	/**
	 * Increasing the charging limit and waiting for EVCS-specific reaction time.
	 */
	INCREASING(3, "Increasing"), //

	/**
	 * Waiting for available power.
	 */
	WAITING_FOR_AVAILABLE_POWER(4, "Waiting for available power");

	private final int value;
	private final String name;

	private ChargeState(int value, String name) {
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