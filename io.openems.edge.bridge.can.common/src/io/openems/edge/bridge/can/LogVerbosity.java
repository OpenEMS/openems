package io.openems.edge.bridge.can;

public enum LogVerbosity {
	/**
	 * Show no logs.
	 */
	NONE,
	/**
	 * Show logs for CAN write requests.
	 */
	WRITES,
	/**
	 * Show verbose logs for received CAN frames.
	 */
	READS,
	/**
	 * Show verbose logs for CAN read and write requests.
	 */
	READS_AND_WRITES,

	/**
	 * Show other CAN relevant debug logs.
	 */
	ALL;

}
