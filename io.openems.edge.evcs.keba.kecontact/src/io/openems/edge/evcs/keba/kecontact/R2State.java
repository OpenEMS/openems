package io.openems.edge.evcs.keba.kecontact;

import io.openems.common.types.OptionsEnum;

public enum R2State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	/**
	 * Startup.
	 */
	STARTUP(0, "Startup"), //
	/**
	 * Not ready for charging. Charging station is not connected to a vehicle, is
	 * locked by the authorization function or another mechanism.
	 */
	NOT_READY(1, "Not ready"), //
	/**
	 * Ready for charging and waiting for reaction from vehicle.
	 */
	READY(2, "Ready"), //
	/**
	 * Charging.
	 */
	CHARGING(3, "Charging"), //
	/**
	 * Error is present.
	 */
	ERROR(4, "Error"), //
	/**
	 * Charging process temporarily interrupted because temperature is too high or
	 * any other voter denies.
	 */
	INTERRUPTED(5, "Interrupted");

	private final int value;
	private final String name;

	private R2State(int value, String name) {
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