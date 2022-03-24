package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum ManualGeneratorStart implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(381, "Stop"), //
	START(1467, "Start");

	private final int value;
	private final String name;

	private ManualGeneratorStart(int value, String name) {
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