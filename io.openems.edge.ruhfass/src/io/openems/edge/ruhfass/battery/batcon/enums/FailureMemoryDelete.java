package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum FailureMemoryDelete implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_DELETE(0, "No Delete"), //
	DELETE(1, "Delete"); //

	private int value;
	private String name;

	private FailureMemoryDelete(int value, String name) {
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
