package io.openems.edge.io.hal.api;

/**
 * Represents an instance of a LED.
 */
public interface Led {
	
	/**
	 * Enables the LED.
	 */
	void on();
	
	/**
	 * Disables the LED.
	 */
	void off();
	
	/**
	 * Switches the LED to a different state.
	 */
	void toggle();
	
	/**
	 * Queries if the LED is currently on or off.
	 * @return true if the LED is currently on, otherwise false.
	 */
	boolean isOn();

	/**
	 * Switches on and off the led periodically.
	 * @param millids how long the LED should be on and off.
	 */
	void blink(int millids);

	/**
	 * Enables the LED for the given time, waits for the time parameter to elapse and disables the LED again. 
	 * @param millis how long the LED should be on in milliseconds.
	 */
	void pulse(int millis);
}
