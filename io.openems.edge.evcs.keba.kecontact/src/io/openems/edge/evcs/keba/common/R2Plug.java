package io.openems.edge.evcs.keba.common;

import io.openems.common.types.OptionsEnum;

public enum R2Plug implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	/**
	 * No cable is plugged.
	 */
	UNPLUGGED(0, "Unplugged"), //
	/**
	 * Cable is plugged into charging station.
	 */
	PLUGGED_ON_EVCS(1, "Plugged on EVCS"), //
	/**
	 * Cable is plugged into charging station and locked.
	 * 
	 * <p>
	 * This is the default idle state for all devices with permanently attached
	 * cable.
	 */
	PLUGGED_ON_EVCS_AND_LOCKED(3, "Plugged on EVCS and locked"), //
	/**
	 * Cable is plugged into charging station and vehicle but not locked.
	 */
	PLUGGED_ON_EVCS_AND_ON_EV(5, "Plugged on EVCS and on EV"), //
	/**
	 * Cable is plugged into charging station and vehicle, furthermore the cable is
	 * locked.
	 * 
	 * <p>
	 * Charging is not possible until plug state "7" is reached.
	 */
	PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED(7, "Plugged on EVCS and on EV and locked");

	private final int value;
	private final String name;

	private R2Plug(int value, String name) {
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