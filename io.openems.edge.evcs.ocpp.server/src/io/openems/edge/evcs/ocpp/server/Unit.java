package io.openems.edge.evcs.ocpp.server;

public enum Unit {

	/**
	 * Watt-hours (energy). Default.
	 */
	WH,

	/**
	 * kiloWatt-hours (energy).
	 */
	KWH,

	/**
	 * Var-hours (reactive energy).
	 */
	VARH,

	/**
	 * kilovar-hours (reactive energy).
	 */
	KVARH,

	/**
	 * Watts (power).
	 */
	W,

	/**
	 * kilowatts (power).
	 */
	KW,

	/**
	 * VoltAmpere (apparent power).
	 */
	VA,

	/**
	 * kiloVolt Ampere (apparent power).
	 */
	KVA,

	/**
	 * Vars (reactive power).
	 */
	VAR,

	/**
	 * kilovars (reactive power).
	 */
	KVAR,

	/**
	 * Amperes (current).
	 */
	A,

	/**
	 * Voltage (r.m.s. AC).
	 */
	V,

	/**
	 * Degrees (temperature).
	 */
	CELSIUS,

	/**
	 * Fahrenheit Degrees (temperature).
	 */
	FAHRENHEIT,

	/**
	 * Kelvin (temperature).
	 */
	K,

	/**
	 * Percentage.
	 */
	PERCENT;
}
