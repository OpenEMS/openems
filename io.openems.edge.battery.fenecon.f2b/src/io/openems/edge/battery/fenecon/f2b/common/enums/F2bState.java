package io.openems.edge.battery.fenecon.f2b.common.enums;

import io.openems.common.types.OptionsEnum;

public enum F2bState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INIT(0, "Init"), //
	WAIT_FOR_VALID_TIMESTAMP(1, "Wait for valid timestamp"), //
	CAN_OFF(2, " Can off"), //
	CAN_ON(3, " Can on"), //
	ERROR(4, "Error"),//
	;//

	private final int value;
	private final String name;

	private F2bState(int value, String name) {
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