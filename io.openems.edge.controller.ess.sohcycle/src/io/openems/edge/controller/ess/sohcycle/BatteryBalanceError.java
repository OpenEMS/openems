package io.openems.edge.controller.ess.sohcycle;

import io.openems.common.types.OptionsEnum;

/**
 * Error/reason why the battery balance check resulted in a particular status.
 * Helps diagnose long-running cycles by persisting the cause when
 * delta cannot be calculated or threshold is exceeded.
 */
public enum BatteryBalanceError implements OptionsEnum {
	NONE(0, "No diagnostic reason (check succeeded or in progress)"),
	BASELINE_MIN_MISSING(1, "Baseline minimum voltage not captured"),
	MAX_VOLTAGE_UNDEFINED(2, "Maximum cell voltage not available from ESS"),
	MIN_VOLTAGE_UNDEFINED(3, "Minimum cell voltage not available from ESS during baseline capture"),
	DELTA_NEGATIVE(4, "Calculated delta is negative (data inconsistency: max < min)"),
	DELTA_ABOVE_THRESHOLD(5, "Voltage delta exceeds allowed threshold"),
	TIMEOUT(6, "Balance check timed out"),
	INTERNAL_ERROR(7, "Internal error during balance check (unexpected exception)");

	private final int value;
	private final String description;

	BatteryBalanceError(int value, String description) {
		this.value = value;
		this.description = description;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	public String getDescription() {
		return this.description;
	}

	@Override
	public OptionsEnum getUndefined() {
		return NONE;
	}
}
