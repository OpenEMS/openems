package io.openems.edge.system.fenecon.masterbox2v0.enums;

import io.openems.common.types.OptionsEnum;

public enum GridState implements OptionsEnum {

	UNDEFINED("Undefined", -1), //
	BATTERY("power from the battery", 0), //
	POWER_SUPPLY("power from power supply/grid", 1);

	private final String name;
	private final int value;

	private GridState(String name, int value) {
		this.name = name;
		this.value = value;
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
