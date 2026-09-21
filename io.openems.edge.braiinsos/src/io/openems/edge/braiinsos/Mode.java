package io.openems.edge.braiinsos;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	OFF(0, "Off"), //
	ON(1, "On"), //
	;

	private final int value;
	private final String name;

	Mode(int value, String name) {
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
		return OFF;
	}
}
