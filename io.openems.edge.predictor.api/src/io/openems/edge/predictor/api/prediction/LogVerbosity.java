package io.openems.edge.predictor.api.prediction;

public enum LogVerbosity {
	/**
	 * Show no logs.
	 */
	NONE,
	/**
	 * Logs all requested predictions.
	 */
	REQUESTED_PREDICTIONS,
	/**
	 * Archive predictions to the local file system.
	 */
	ARCHIVE_LOCALLY;
}
