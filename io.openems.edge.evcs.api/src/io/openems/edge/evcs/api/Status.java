package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STARTING(0, "Starting"), //
	/**
	 * e.g. unplugged, RFID not enabled,...
	 */
	NOT_READY_FOR_CHARGING(1, "Not ready for Charging"), //
	/**
	 * Waiting for EV charging request.
	 */
	READY_FOR_CHARGING(2, "Ready for Charging"), //
	CHARGING(3, "Charging"), //
	ERROR(4, "Error"), //
	CHARGING_REJECTED(5, "Charging rejected"), //
	ENERGY_LIMIT_REACHED(6, "The charging limit reached"), //
	CHARGING_FINISHED(7, "Charging has finished");

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