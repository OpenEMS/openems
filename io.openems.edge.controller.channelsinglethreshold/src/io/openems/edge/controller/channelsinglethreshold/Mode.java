package io.openems.edge.controller.channelsinglethreshold;

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
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		// TODO Auto-generated method stub
		return AUTOMATIC;
	}
}