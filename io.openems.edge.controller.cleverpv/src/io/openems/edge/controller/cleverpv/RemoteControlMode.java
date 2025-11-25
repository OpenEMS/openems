package io.openems.edge.controller.cleverpv;

import io.openems.common.types.OptionsEnum;

public enum RemoteControlMode implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	NO_DISCHARGE(1, "No discharge"), //
	CHARGE_FROM_GRID(2, "Charge from grid");

	private final int value;
	private final String name;

	private RemoteControlMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public OptionsEnum getUndefined() {
		return RemoteControlMode.UNDEFINED;
	}

	@Override
	public String getName() {
		return this.name;
	}
}
