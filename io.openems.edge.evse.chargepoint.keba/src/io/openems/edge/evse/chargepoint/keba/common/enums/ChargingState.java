package io.openems.edge.evse.chargepoint.keba.common.enums;

import io.openems.common.types.OptionsEnum;

public enum ChargingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STARTING(0, "Start-up of the charging station"), //
	/**
	 * The charging station is not ready for charging. The charging station is not
	 * connected to an electric vehicle, it is locked by the authorization function
	 * or another mechanism.
	 */
	NOT_READY_FOR_CHARGING(1, "Not ready for Charging"), //
	/**
	 * The charging station is ready for charging and waits for a reaction from the
	 * electric vehicle.
	 */
	READY_FOR_CHARGING(2, "Ready for Charging & Waiting for EV"), //
	/**
	 * A charging process is active.
	 */
	CHARGING(3, "Charging"), //
	/**
	 * An error has occurred.
	 */
	ERROR(4, "Error"), //
	/**
	 * The charging process is temporarily interrupted because the temperature is
	 * too high or the wallbox is in suspended mode.
	 */
	INTERRUPTED(5, "Charging is temporarily interrupted"); //

	private final int value;
	private final String name;

	private ChargingState(int value, String name) {
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