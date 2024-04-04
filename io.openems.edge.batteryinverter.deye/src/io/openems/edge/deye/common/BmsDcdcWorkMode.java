package io.openems.edge.deye.common;

import io.openems.common.types.OptionsEnum;

public enum BmsDcdcWorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CONSTANT_CURRENT(128, "Constant Current"), //
	CONSTANT_VOLTAGE(256, "Constant Voltage"), //
	BOOST_MPPT(512, "Boost MPPT"); //

	private final int value;
	private final String name;

	private BmsDcdcWorkMode(int value, String name) {
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