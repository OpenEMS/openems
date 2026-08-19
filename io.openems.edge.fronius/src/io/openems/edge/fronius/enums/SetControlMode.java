package io.openems.edge.fronius.enums;

import io.openems.common.types.OptionsEnum;

public enum SetControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	CHARGE_LIMIT(1, "Charge limit"), //
	DISCHARGE_LIMIT(2, "Discharge limit"), //
	CHARGE_AND_DISCHARGE_LIMIT(3, "Charge and discharge limit");

	private final int value;
	private final String name;

	SetControlMode(int value, String name) {
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
