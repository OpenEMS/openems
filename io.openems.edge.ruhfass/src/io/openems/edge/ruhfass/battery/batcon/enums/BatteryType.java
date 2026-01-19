package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	A3(1, "A3"), //
	Q7(2, "Q7"), //
	E_UP(3, "e-UP"), //
	CBEV(4, "CBEV"), //
	GTE(5, "GTE"); //

	private int value;
	private String name;

	private BatteryType(int value, String name) {
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
