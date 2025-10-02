package io.openems.edge.controller.cleverpv;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {

	/**
	 * Unknown state.
	 */
	UNDEFINED(-1, "Undefined"),

	OFF(0, "Off"),

	NO_DISCHARGE(1, "No discharge");

	private final int value;
	private final String name;

	ControlMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public OptionsEnum getUndefined() {
		return ControlMode.UNDEFINED;
	}

	@Override
	public String getName() {
		return this.name;
	}
}
