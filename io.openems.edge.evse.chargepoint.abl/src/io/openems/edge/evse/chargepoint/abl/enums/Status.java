package io.openems.edge.evse.chargepoint.abl.enums;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_READY_FOR_CHARGING(0, "Not ready for charging"), //
	READY_FOR_CHARGING(1, "Ready for charging"), //
	CHARGING(2, "Charging"), //
	ERROR(3, "Error"); //

	private final int value;
	private final String name;

	private Status(int value, String name) {
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
