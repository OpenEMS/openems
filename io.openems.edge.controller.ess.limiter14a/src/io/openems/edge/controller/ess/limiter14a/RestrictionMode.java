package io.openems.edge.controller.ess.limiter14a;

import io.openems.common.types.OptionsEnum;

public enum RestrictionMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ON(1, "On"), //
	OFF(0, "Off");

	private int value;
	private String name;

	private RestrictionMode(int value, String name) {
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