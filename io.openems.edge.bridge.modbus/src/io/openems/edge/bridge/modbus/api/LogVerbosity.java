package io.openems.edge.bridge.modbus.api;

public enum LogVerbosity {
	/**
	 * Show no logs.
	 */
	NONE,
	/**
	 * Show basic information in Controller.Debug.Log.
	 */
	DEBUG_LOG,
	/**
	 * Show logs for all read and write requests.
	 */
	READS_AND_WRITES,
	/**
	 * Show logs for all read and write requests, including actual hex values of
	 * request and response.
	 */
	READS_AND_WRITES_VERBOSE,
	/**
	 * Show logs for all read and write requests, including actual duration time per
	 * request.
	 */
	READS_AND_WRITES_DURATION,
	/**
	 * Show logs for all read and write requests, including actual duration time per
	 * request & trace the internal Event-based State-Machine.
	 */
	READS_AND_WRITES_DURATION_TRACE_EVENTS;
}
