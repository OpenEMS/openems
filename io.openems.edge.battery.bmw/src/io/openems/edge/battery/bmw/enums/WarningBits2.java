package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum WarningBits2 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CONTAINER(0, "Container Warning"), //
	SOH(1, "SoH Warning"), //
	RACK_STRING(2, "Rack/String Warning");

	private final int value;
	private final String name;

	private WarningBits2(int value, String name) {
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
