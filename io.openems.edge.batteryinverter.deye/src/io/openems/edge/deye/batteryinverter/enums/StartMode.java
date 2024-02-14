package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum StartMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	MANUAL(0, "Manual Start"), //
	AUTO(1, "Auto Start"); //

	private final int value;
	private final String name;

	private StartMode(int value, String name) {
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