package io.openems.edge.controller.chp.soc;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	MANUAL_ON(0, "Manual control for the ON signal"), //
	MANUAL_OFF(1, "Manual control for the OFF signal"), //
	AUTOMATIC(2, "Automatic control"); //

	private final int value;
	private final String name;

	private Mode(int value, String name) {
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
		return AUTOMATIC;
	}
}