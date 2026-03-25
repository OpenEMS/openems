package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EnableDisable implements OptionsEnum {

	DISABLE(0, "Disable", false), //
	ENABLE(1, "Enable", true);

	private final int value;
	private final String name;
	public final boolean booleanValue;

	private EnableDisable(int value, String name, boolean booleanValue) {
		this.value = value;
		this.name = name;
		this.booleanValue = booleanValue;
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
		return null;
	}
}
