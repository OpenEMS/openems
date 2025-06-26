package io.openems.edge.batteryinverter.refu88k.enums;

import io.openems.common.types.OptionsEnum;

public enum OutPfSetEna implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	ENABLED(1, "Enabled");

	private final int value;
	private final String name;

	private OutPfSetEna(int value, String name) {
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
