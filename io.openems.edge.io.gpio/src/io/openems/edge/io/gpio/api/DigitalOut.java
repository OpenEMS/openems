package io.openems.edge.io.gpio.api;

import io.openems.common.exceptions.OpenemsException;

public interface DigitalOut extends DigitalIn {

	/**
	 * Sets the value of the GPIO according to the input parameter.
	 * 
	 * @param value sets the GPIO to high if value is true, otherwise sets it to
	 *              low.
	 * @throws OpenemsException exception if setting the value is not successful.
	 */
	public void setValue(boolean value) throws OpenemsException;
}
