package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum SinexcelGridMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON_GRID(0, "On Grid"), //
	OFF_GRID(1, "Off Grid"); //

	private final int value;
	private final String name;

	private SinexcelGridMode(int value, String name) {
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