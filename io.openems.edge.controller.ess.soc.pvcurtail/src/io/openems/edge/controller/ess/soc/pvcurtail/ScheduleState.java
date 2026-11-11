package io.openems.edge.controller.ess.soc.pvcurtail;

import io.openems.common.types.OptionsEnum;

/** What the active power control is doing this cycle. */
public enum ScheduleState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SCENARIO_1(0, "Scenario 1: POI setpoint"), //
	CHARGE(1, "Scenario 2: charge window"), //
	CHARGE_SOC_LIMIT(2, "Scenario 2: charge window, SoC limit reached"), //
	DISCHARGE(3, "Scenario 2: discharge window"), //
	DISCHARGE_SOC_LIMIT(4, "Scenario 2: discharge window, SoC limit reached"), //
	OUTSIDE_WINDOWS(5, "Scenario 2: outside the windows, base POI"), //
	OFF(6, "Off");

	private final int value;
	private final String name;

	private ScheduleState(int value, String name) {
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
