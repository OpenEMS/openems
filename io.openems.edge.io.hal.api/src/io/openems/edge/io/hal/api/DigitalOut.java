package io.openems.edge.io.hal.api;

public interface DigitalOut extends HardwareComponent {
	
	/**
	 * Indicates if the state of the digital output is HIGH.
	 * @return true if the output is HIGH, otherwise false.
	 */
	boolean isOn();
	
	/**
	 * Indicates if the state of the digital output is LOW.
	 * @return true if the output is LOW, otherwise false.
	 */
	boolean isOff();

	/**
	 * Inverts the state of the digital output.
	 */
	void toggle();
	
	/**
	 * Sets the digital output to HIGH state.
	 */
	void setOn();
	
	/**
	 * Sets the digital output to LOW state.
	 */
	void setOff();
}
