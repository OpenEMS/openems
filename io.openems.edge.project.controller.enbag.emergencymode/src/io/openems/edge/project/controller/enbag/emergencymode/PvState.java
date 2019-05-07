package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.common.types.OptionsEnum;

public enum PvState implements OptionsEnum {
	UNKNOWN(-1, "Unknown"), //
	SUFFICIENT(0, ""), //
	NOT_SUFFICIENT(1, "");

	private final int value;
	private final String name;

	private PvState(int value, String name) {
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
		return UNKNOWN;
	}
}