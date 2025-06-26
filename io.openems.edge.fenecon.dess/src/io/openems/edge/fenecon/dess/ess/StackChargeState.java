package io.openems.edge.fenecon.dess.ess;

import io.openems.common.types.OptionsEnum;

public enum StackChargeState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDBY(0, "Standby"), //
	CHARGING(1, "Charging"), //
	DISCHARGING(2, "Discharging");

	private final int value;
	private final String name;

	private StackChargeState(int value, String name) {
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