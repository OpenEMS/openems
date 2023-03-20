package io.openems.edge.io.hal.api;

public interface HardwareComponent {
	
	/**
	 * Frees the underlying hardware lock of the component.
	 */
	void release();
	
}
