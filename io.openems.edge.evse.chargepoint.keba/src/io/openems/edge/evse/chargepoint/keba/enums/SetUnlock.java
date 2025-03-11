package io.openems.edge.evse.chargepoint.keba.enums;

import io.openems.common.types.OptionsEnum;

public enum SetUnlock implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNLOCK(0, "Set Unlock");

	private final int value;
	private final String name;

	private SetUnlock(int value, String name) {
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