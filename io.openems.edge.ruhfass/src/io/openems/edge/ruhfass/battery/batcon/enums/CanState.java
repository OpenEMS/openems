package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum CanState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INACTIVE(0, "Can Message Updated"), //
	ACTIVE(1, "Can Message not Updated for more than 2sec"); //

	private int value;
	private String name;

	private CanState(int value, String name) {
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
