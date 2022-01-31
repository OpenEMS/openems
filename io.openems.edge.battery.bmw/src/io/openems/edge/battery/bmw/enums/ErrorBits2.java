package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum ErrorBits2 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CONTAINER(0, "Container Error"), //
	SOH(1, "SoH Error"), //
	RACK_STRING(2, "Rack/String Error");

	private final int value;
	private final String name;

	private ErrorBits2(int value, String name) {
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
