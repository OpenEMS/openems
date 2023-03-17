package io.openems.edge.io.hal.api;

/**
 * Represents an instance of a LED.
 */
public interface Led extends HardwareComponent {
	
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
}
