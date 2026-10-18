package io.openems.common.logger;

import java.util.Map;

import com.google.gson.JsonElement;

public interface DebugLoggable {

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output; null for no log
	 */
	String debugLog();

	/**
	 * Gets some output that is suitable for a debug metrics to write down as
	 * Timedata. Currently only used in Backend.
	 * 
	 * @return the key value entries to write down; null or emptyMap for no metrics
	 */
	default Map<String, JsonElement> debugMetrics() {
		return null;
	}

}
