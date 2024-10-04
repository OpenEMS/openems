package io.openems.edge.controller.ess.chargedischargelimiter;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // SoC in range between min and max
	ERROR(1, "Error State"), //
	BELOW_MIN_SOC(2, "Min-SoC"), //
	ABOVE_MAX_SOC(3, "Max-SoC"), //
	FORCE_CHARGE_ACTIVE(4, "Force-Charge-SoC"), // ESS is charging to configured balancing point
	BALANCING_WANTED(5, "Balancing wanted"),
	BALANCING_ACTIVE(6, "Balancing active");

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