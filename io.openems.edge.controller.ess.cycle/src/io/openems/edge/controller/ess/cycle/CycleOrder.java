package io.openems.edge.controller.ess.cycle;

import io.openems.common.types.OptionsEnum;

public enum CycleOrder implements OptionsEnum {
	AUTO(-1, "Soc < 50 %: Discharge; otherwise Charge"), //
	START_WITH_CHARGE(0, "Start with Charge"), //
	START_WITH_DISCHARGE(1, "Start with Discharge");

	private final int value;
	private final String name;

	private CycleOrder(int value, String name) {
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
		return AUTO;
	}
}