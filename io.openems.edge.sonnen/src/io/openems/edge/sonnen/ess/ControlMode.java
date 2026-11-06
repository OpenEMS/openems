package io.openems.edge.sonnen.ess;

/**
 * Control modes for the sonnenBatterie ESS.
 */
public enum ControlMode {

	/**
	 * Uses the internal operating mode of the sonnenBatterie (e.g.
	 * Self-Consumption). Allows no remote control of Set-Points.
	 */
	INTERNAL,

	/**
	 * Uses the internal operating mode of the sonnenBatterie, but smartly
	 * switches to 'Manual' mode with a Set-Point if required, e.g. when the
	 * OpenEMS Balancing controller requests a deviation from the internal
	 * behavior. Requires a sonnenBatterie Smart Meter at the grid junction
	 * point.
	 */
	SMART,

	/**
	 * Full control of the sonnenBatterie by OpenEMS via 'Manual' mode and
	 * charge/discharge Set-Points.
	 */
	REMOTE;
}
