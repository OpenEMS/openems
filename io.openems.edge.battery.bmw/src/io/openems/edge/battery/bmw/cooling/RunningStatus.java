package io.openems.edge.battery.bmw.cooling;

import io.openems.common.types.OptionsEnum;

public enum RunningStatus implements OptionsEnum {
	UNDEFINED("Undefined", -1), //
	OFF("Off", 0), //
	ON("On", 1), //
	;

	private RunningStatus(String name, int value) {
		this.name = name;
		this.value = value;
	}

	private int value;
	private String name;

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

