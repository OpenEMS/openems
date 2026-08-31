package io.openems.edge.sungrow.ess.enums;

public enum ControlMode {

	/**
	 * Uses the internal 'SELF_CONSUMPTION' mode of the Sungrow inverter. Allows no remote
	 * control of Set-Points. Requires a Sungrow Meter at the grid junction
	 * point.
	 */
	INTERNAL,
	/**
	 * Uses the internal 'SELF_CONSUMPTION' mode of the Sungrow inverter but smartly switches to
	 * other modes if required. Requires a Sungrow Meter at the grid junction
	 * point.
	 */
	SMART,
	/**
	 * Full control of the Sungrow inverter by OpenEMS. Slower than internal 'SELF_CONSUMPTION'
	 * mode, but does not require a Sungrow Meter at the grid junction point.
	 */
	REMOTE;

}