package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STARTING(0, "Starting"), //
	NOT_READY_FOR_CHARGING(1, "Not ready for Charging"), // e.g. unplugged, X1 or "ena" not enabled, RFID not
															// enabled,...
	READY_FOR_CHARGING(2, "Ready for Charging"), // waiting for EV charging request
	CHARGING(3, "Charging"), //
	ERROR(4, "Error"), //
	AUTHORIZATION_REJECTED(5, "Authorization rejected");

	private final int value;
	private final String name;

	private Status(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}