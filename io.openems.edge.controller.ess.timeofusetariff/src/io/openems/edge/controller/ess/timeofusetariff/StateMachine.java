package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.types.OptionsEnum;

public enum StateMachine implements OptionsEnum {
	/*
	 * BALANCING mode is first = default.
	 */
	BALANCING(1, "Self-consumption optimization"), //
	DELAY_DISCHARGE(0, "Delay discharge"), //
	CHARGE_GRID(3, "Charge from grid"), //
	DISCHARGE_GRID(4, "Discharge to grid"), //
	/*
	 * Peak-Shaving internally does the same as CHARGE_GRID.
	 */
	PEAK_SHAVING(5, "Grid Peak-Shaving") //
	;

	private final int value;
	private final String name;

	private StateMachine(int value, String name) {
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
		return BALANCING;
	}

}
