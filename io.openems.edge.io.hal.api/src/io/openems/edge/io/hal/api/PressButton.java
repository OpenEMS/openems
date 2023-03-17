package io.openems.edge.io.hal.api;

/**
 * Represents a press button with active-low configuration.
 *
 */
public interface PressButton extends HardwareComponent {
	
	/**
	 * Queries the button if it is pressed at the given time.
	 * @return true if the button is currently pressed otherwise false.
	 */
	boolean isPressed();
	
}
