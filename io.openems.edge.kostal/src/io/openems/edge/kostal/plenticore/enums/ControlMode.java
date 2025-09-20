package io.openems.edge.kostal.plenticore.enums;

public enum ControlMode {

	/**
	 * Uses the internal 'AUTO' mode of the inverter. Allows no remote control of
	 * Set-Points. Requires a Smart Meter at the grid junction point.
	 */
	INTERNAL,
	/**
	 * Full control of the inverter by OpenEMS. Slower than internal 'AUTO' mode,
	 * but does not require a Smart Meter at the grid junction point.
	 */
	REMOTE,
	/**
	 * Uses the internal 'AUTO' mode of the inverter. Allows remote control of
	 * Set-Points based on differences. Requires a Smart Meter at the grid junction
	 * point.
	 */
	SMART;
}
