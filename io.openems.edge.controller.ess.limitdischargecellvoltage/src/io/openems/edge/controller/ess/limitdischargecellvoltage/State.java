package io.openems.edge.controller.ess.limitdischargecellvoltage;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), //
	WARNING(1, "Warning"), //
	CRITICAL(2, "Critical"), //
	CHARGE(3, "Charge");
	

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