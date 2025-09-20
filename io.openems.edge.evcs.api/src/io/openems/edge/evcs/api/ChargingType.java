package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum ChargingType implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	CCS(0, "CCS"), //
	CHADEMO(1, "Chademo"), //
	AC(2, "AC");

	private final int value;
	private final String name;

	private ChargingType(int value, String name) {
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
