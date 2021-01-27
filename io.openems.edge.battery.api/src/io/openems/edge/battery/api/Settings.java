package io.openems.edge.battery.api;

public interface Settings {

	/**
	 * Gets the max increase in milli ampere.
	 * @return int
	 */
	int getMaxIncreaseMilliAmpere();

	/**
	 * Gets the power factor.
	 * @return double
	 */
	double getPowerFactor();

	/**
	 * Gets the minimal current in ampere.
	 * @return double
	 */
	double getMinimumCurrentAmpere();

	/**
	 * Gets the tolerance in millivolt.
	 * @return int
	 */
	int getToleranceMilliVolt();

}