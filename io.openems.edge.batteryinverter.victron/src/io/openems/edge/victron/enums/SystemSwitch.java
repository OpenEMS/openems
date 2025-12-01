package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemSwitch implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	ENABLED(1, "Enabled");

	private final int value;
	private final String name;

	private SystemSwitch(int value, String name) {
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