package io.openems.edge.aircooling.seifert.enums;

import io.openems.common.types.OptionsEnum;

public enum ModeState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	IDLE(1, "Idle"), //
	COOLING(2, "Cooling"), //
	HEATING(3, "Heating"), //
	TESTING(4, "Testing"), //
	;

	private final int value;
	private final String name;

	ModeState(int value, String name) {
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
