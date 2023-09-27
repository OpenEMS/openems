package io.openems.edge.bridge.mqtt.api;

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
	READS_AND_WRITES;
}
