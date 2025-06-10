package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum LoadMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON(0, "ON,inverter connects to Load"), //
	OFF(1, "OFF, inverter disconnects to Load"); //

	private final int value;
	private final String option;

	private LoadMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}