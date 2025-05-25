package io.openems.edge.evse.chargepoint.keba.common.enums;

import io.openems.common.types.OptionsEnum;

public enum CableState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNPLUGGED(0, "Unplugged"), //
	/**
	 * Cable is connected to the charging station (not to the electric vehicle).
	 */
	PLUGGED_ON_WALLBOX(1, "Plugged on wallbox (not EV)"), //
	/**
	 * Cable is connected to the charging station and locked (not to the electric
	 * vehicle).
	 */
	PLUGGED_ON_WALLBOX_AND_LOCKED(3, "Plugged on wallbox and locked (not EV)"), //
	/**
	 * Cable is connected to the charging station and the electric vehicle (not
	 * locked).
	 */
	PLUGGED_EV_NOT_LOCKED(5, "Plugged, not locked on EV"), //
	/**
	 * Cable is connected to the charging station and the electric vehicle and
	 * locked (charging).
	 */
	PLUGGED_AND_LOCKED(7, "Plugged and locked");

	private final int value;
	private final String name;

	private CableState(int value, String name) {
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