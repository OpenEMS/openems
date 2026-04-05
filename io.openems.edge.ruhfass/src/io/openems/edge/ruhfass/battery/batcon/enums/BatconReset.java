package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BatconReset implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_RESET(0, "No Reset"), //
	RESET(1, "Reset"); //

	private int value;
	private String name;

	private BatconReset(int value, String name) {
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
