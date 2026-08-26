package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EnableDisableOrUndefined implements OptionsEnum {

	UNDEFINED(-1, "Undefined", null), //
	DISABLE(0, "Disable", false), //
	ENABLE(1, "Enable", true); //

	public final Boolean booleanValue;

	private final int value;
	private final String name;

	private EnableDisableOrUndefined(int value, String name, Boolean booleanValue) {
		this.value = value;
		this.name = name;
		this.booleanValue = booleanValue;
	}

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
