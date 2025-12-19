package io.openems.edge.controller.ess.chargedischargelimiter;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // SoC in range between min and max
	ERROR(1, "Error State"), //
	BELOW_MIN_SOC(2, "Below configured Min-SoC"), //
	ABOVE_MAX_SOC(3, "Above configured Max-SoC"), //
	MIN_SOC_REACHED(4, "Minimum SoC limit reached"),
	MAX_SOC_REACHED(5, "Maximum SoC limit reached"),	
	FORCE_CHARGE_ACTIVE(6, "Force-Charge-to-SoC"), // ESS is charging to configured balancing point
	BALANCING_WANTED(7, "Balancing wanted"),
	BALANCING_ACTIVE(8, "Balancing active"),
	PRICE_LIMIT(9, "Price exceeds configured limit"),
	APPROACHING_MIN_SOC(10, "SoC approaching minimum limit. Decreasing power"),
	APPROACHING_MAX_SOC(11, "SoC approaching maximum limit. Decreasing power"),;


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