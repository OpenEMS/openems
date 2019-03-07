package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum EssState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //

	OFF_GRID(0, "Ess In Off Grid Mode"), //

	ON_GRID(1, "Ess In On Grid Mode");

	private final int value;
	private final String name;

	private EssState(int value, String name) {
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
		return UNDEFINED;
	}
}