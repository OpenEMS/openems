package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum ShortCircuitFunction implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ENABLE(0x1, "Enable"), //
	DISABLE(0x2, "Disable");

	private final int value;
	private final String name;

	private ShortCircuitFunction(int value, String name) {
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