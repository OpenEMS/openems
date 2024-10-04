package io.openems.edge.controller.ess.chargedischargelimiter;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	MIN_SOC(1, "Min-SoC"), //
	MAX_SOC(2, "Max-SoC"), //
	FORCE_CHARGE_SOC(3, "Force-Charge-SoC"), //
	FORCE_DISCHARGE_SOC(4, "Force-Discharge-SoC");

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