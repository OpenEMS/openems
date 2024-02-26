package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.types.OptionsEnum;

public enum StateMachine implements OptionsEnum {
	DELAY_DISCHARGE(0, "Delay discharge"), //
	BALANCING(1, "Self-consumption optimization"), //
	CHARGE_GRID(3, "Charge from grid") //
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
