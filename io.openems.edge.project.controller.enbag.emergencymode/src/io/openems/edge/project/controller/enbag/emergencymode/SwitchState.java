package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum SwitchState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //

	SWITCH_ALL_OPEN(0, "All Switches Are Open"), //

	SWITCH_AT_OFF_GRID(1, "Switches Are At Off Grid Mode"), //

	SWITCH_AT_ON_GRID(2, "Switches Are At On Grid Mode");

	private final int value;
	private final String name;

	private SwitchState(int value, String name) {
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