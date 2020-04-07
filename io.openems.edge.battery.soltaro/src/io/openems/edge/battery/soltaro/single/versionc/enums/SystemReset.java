package io.openems.edge.battery.soltaro.single.versionc.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemReset implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACTIVATE(0x1, "Activates the Reset");

	private final int value;
	private final String name;

	private SystemReset(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
