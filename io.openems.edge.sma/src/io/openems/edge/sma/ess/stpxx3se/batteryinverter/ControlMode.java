package io.openems.edge.sma.ess.stpxx3se.batteryinverter;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {

	/**
	 * Full control of the SMA inverter by OpenEMS. Slower than internal mode, but
	 * does not require an SMA Sunny Home Manager at the grid junction point.
	 */
	REMOTE(802, "Remote"),
	/**
	 * Uses the internal mode of the SMA inverter. Allows no remote control of
	 * Set-Points. Requires an SMA Sunny Home Manager at the grid junction point.
	 */
	INTERNAL(803, "Internal"),
	/**
	 * Uses the internal 'AUTO' mode of the GoodWe inverter but smartly switches to
	 * other modes if required.Requires an SMA Sunny Home Manager at the grid
	 * junction point.
	 */
	SMART(804, "Smart");

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
		return INTERNAL;
	}

}
