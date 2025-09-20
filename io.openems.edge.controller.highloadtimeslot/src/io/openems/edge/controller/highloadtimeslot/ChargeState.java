package io.openems.edge.controller.highloadtimeslot;

enum ChargeState {
	/**
	 * Normal charge state: charge till the battery is full.
	 */
	NORMAL,
	/**
	 * Hysteresis charge state: block charging after 'Normal charge' till the
	 * battery is not anymore completely full.
	 */
	HYSTERESIS,
	/**
	 * Force charge state: force full charging just before the high-load timeslot
	 * starts.
	 */
	FORCE_CHARGE;
}