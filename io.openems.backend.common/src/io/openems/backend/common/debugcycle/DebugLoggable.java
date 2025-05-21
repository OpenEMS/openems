package io.openems.backend.common.debugcycle;

import java.util.Map;

import com.google.gson.JsonElement;

import io.openems.backend.common.timedata.Timedata;

public interface DebugLoggable {

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output; null for no log
	 */
	public String debugLog();

	/**
	 * Gets some output that is suitable for a debug metrics to write down as
	 * {@link Timedata}.
	 * 
	 * @return the key value entries to write down; null or emptyMap for no metrics
	 */
	public Map<String, JsonElement> debugMetrics();

}
