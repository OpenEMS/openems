package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum Switch implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_SWITCH(0, "No Switch"), //
	SWITCH(1, "Switch");//

	private final int value;
	private final String name;

	private Switch(int value, String name) {
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