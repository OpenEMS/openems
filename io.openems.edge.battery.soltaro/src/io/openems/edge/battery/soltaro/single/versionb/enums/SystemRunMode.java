package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemRunMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0x1, "Normal"), //
	DEBUG(0x2, "Debug");

	private final int value;
	private final String name;

	private SystemRunMode(int value, String name) {
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