package io.openems.edge.battery.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public interface BatteryHeatable extends Battery {
	/**
	 * A helper method in determining when to begin heating the battery. The serial
	 * cluster will begin to heat up the batteries if the all battery temperatures
	 * is below 10 degrees and they are all started.
	 * 
	 * @param value true for start heating.
	 */
	public void setHeatingTarget(boolean value);

	/**
	 * Start the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void startHeating() {
		this.setHeatingTarget(true);
	}

	/**
	 * Stop the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void stopHeating() {
		this.setHeatingTarget(false);
	}
}
