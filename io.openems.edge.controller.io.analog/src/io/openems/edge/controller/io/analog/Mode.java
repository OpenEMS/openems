package io.openems.edge.controller.io.analog;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	ON(0, "ON signal"), //
	OFF(1, "OFF signal"), //
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