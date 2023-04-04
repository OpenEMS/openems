package io.openems.edge.bridge.modbus.api;

public enum LogVerbosity {
	/**
	 * Show now logs.
	 */
	NONE,
	/**
	 * Show logs for modbus write requests.
	 */
	WRITES,
	/**
	 * Show verbose logs for modbus read and write requests.
	 */
	READS_AND_WRITES,
	// TODO Remove before release
	DEV_REFACTORING;
}
