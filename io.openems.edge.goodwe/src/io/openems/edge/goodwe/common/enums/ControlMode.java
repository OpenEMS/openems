package io.openems.edge.goodwe.common.enums;

public enum ControlMode {

	/**
	 * Uses the internal 'AUTO' mode of the GoodWe inverter. Allows no remote
	 * control of Set-Points. Requires a GoodWe Smart Meter at the grid junction
	 * point.
	 */
	INTERNAL,
	/**
	 * Uses the internal 'AUTO' mode of the GoodWe inverter but smartly switches to
	 * other modes if required.Requires a GoodWe Smart Meter at the grid junction
	 * point.
	 */
	SMART,
	/**
	 * Full control of the GoodWe inverter by OpenEMS. Slower than internal 'AUTO'
	 * mode, but does not require a GoodWe Smart Meter at the grid junction point.
	 */
	REMOTE;

}
