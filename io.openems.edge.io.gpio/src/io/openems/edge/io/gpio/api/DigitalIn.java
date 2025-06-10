package io.openems.edge.io.gpio.api;

import java.util.Optional;

public interface DigitalIn {

	/**
	 * Gets the value of the digital input.
	 * 
	 * @return true if the input is high, otherwise false. Undefined on
	 *         error/unknown.
	 */
	Optional<Boolean> getValue();

}
