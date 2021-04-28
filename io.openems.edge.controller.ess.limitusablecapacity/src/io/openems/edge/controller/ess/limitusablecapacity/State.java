package io.openems.edge.controller.ess.limitusablecapacity;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_LIMIT(0, "No limit"), //
	STOP_DISCHARGE(1, "Stopping Discharge"), //
	FORCE_CHARGE(2, "Force-Charge"), //
	STOP_CHARGE(3, "Stopping Charge"); //

	private final int value;
	private final String name;

	private State(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
