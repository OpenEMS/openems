package io.openems.edge.evse.chargepoint.keba.enums;

import io.openems.common.types.OptionsEnum;

public enum SetEnable implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Disable charging station (Suspended mode)"), //
	ENABLE(1, "Enable charging station (Charging)");

	private final int value;
	private final String name;

	private SetEnable(int value, String name) {
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