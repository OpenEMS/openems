package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactorCommand implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OPEN(0, "Open"), //
	CLOSE(1, "Close"); //

	private int value;
	private String name;

	private ContactorCommand(int value, String name) {
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
