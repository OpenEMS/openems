package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum BlackStartMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL_STARTUP(0, "Normal Start Up"), //
	BLACK_STARTUP(1, "Black Start Up");//

	private final int value;
	private final String name;

	private BlackStartMode(int value, String name) {
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