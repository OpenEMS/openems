package io.openems.edge.battery.fenecon.f2b.common.enums;

import io.openems.common.types.OptionsEnum;

public enum F2bCanCommunication implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CAN_OFF(0, "CAN OFF"), //
	CAN_ON(1, "CAN ON"), //
	;//

	private final int value;
	private final String name;

	private F2bCanCommunication(int value, String name) {
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