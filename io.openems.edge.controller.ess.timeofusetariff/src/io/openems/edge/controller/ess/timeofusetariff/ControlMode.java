package io.openems.edge.controller.ess.timeofusetariff;

public enum ControlMode {
	/**
	 * Charge consumption from the grid.
	 */
	CHARGE_CONSUMPTION, //
	/**
	 * Delays discharge during low-price hours.
	 */
	DELAY_DISCHARGE; //
}