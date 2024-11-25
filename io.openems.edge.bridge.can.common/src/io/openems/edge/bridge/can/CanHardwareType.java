package io.openems.edge.bridge.can;

public enum CanHardwareType {
	/**
	 * uses the software simulation.
	 */
	SIMULATOR,

	/**
	 * Uses the SocketCAN driver.
	 */
	SOCKETCAN;

}
