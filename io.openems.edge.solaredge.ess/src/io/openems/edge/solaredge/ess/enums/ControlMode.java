package io.openems.edge.solaredge.ess.enums;

public enum ControlMode {

	/**
	 * Uses the internal 'AUTO' mode of the SolarEdge inverter. Allows no remote
	 * control of Set-Points. Requires a SolarEdge Smart Meter at the grid junction
	 * point.
	 */
	INTERNAL,
	/**
	 * Uses the internal 'AUTO' mode of the SolarEdge inverter but smartly switches to
	 * other modes if required. Requires a SolarEdge Smart Meter at the grid junction
	 * point.
	 */
	SMART,
	/**
	 * Full control of the SolarEdge inverter by OpenEMS. Slower than internal 'AUTO'
	 * mode, but does not require a SolarEdge Smart Meter at the grid junction point.
	 */
	REMOTE;

}