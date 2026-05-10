package io.openems.edge.system.fenecon.masterbox2v0.enums;

import io.openems.common.types.OptionsEnum;

public enum StateEnergyMeter implements OptionsEnum {

	NO_ERROR("No Error", 0), //
	ERROR("Error", 1), //
	UNDEFINED("Undefined", 256);

	private final String name;
	private final int value;

	private StateEnergyMeter(String name, int value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
