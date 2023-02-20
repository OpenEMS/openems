package io.openems.edge.io.hal.api;

/**
 * Represents a callback function for a button press.
 *
 */
@FunctionalInterface
public interface ButtonPressCallback {
	
	/**
	 * This function gets called when the button is pressed.
	 */
	void onButtonPress();
}
