package io.openems.edge.protectionrelay.telehaase.na003m64.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMALLY_OPENED(0, "Normally opened"), //
	NORMALLY_CLOSED(1, "Normally closed"), //
	DISABLED(2, "Disabled"), //
	;

	private final int value;
	private final String name;

	ContactState(int value, String name) {
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
