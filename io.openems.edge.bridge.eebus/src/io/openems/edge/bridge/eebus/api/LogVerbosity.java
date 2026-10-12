package io.openems.edge.bridge.eebus.api;

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

	;

	public boolean isDebug() {
		return switch (this) {
			case DEBUG_LOG, READS_AND_WRITES -> true;
			case NONE -> false;
		};
	}

	public boolean isReadsAndWrites() {
		return switch (this) {
			case READS_AND_WRITES -> true;
			case DEBUG_LOG, NONE -> false;
		};
	}
}
