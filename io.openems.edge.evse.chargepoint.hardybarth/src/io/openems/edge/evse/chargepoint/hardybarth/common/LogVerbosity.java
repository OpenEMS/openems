package io.openems.edge.evse.chargepoint.hardybarth.common;

public enum LogVerbosity {
	/**
	 * No Logging.
	 */
	NONE,
	/**
	 * Show basic information in Controller.Debug.Log.
	 */
	DEBUG_LOG,
	/**
	 * Show logs for writes.
	 */
	WRITES,
	/**
	 * Show detailed logs for reads.
	 */
	READS;
}
