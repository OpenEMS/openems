package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum FanStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OPEN(0x1, "Open"), //
	CLOSE(0x2, "Close");

	private final int value;
	private final String name;

	private FanStatus(int value, String name) {
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