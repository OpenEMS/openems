package io.openems.backend.common.debugcycle;

import static java.util.Collections.emptyMap;

import java.util.Map;

import com.google.gson.JsonElement;

import io.openems.backend.common.timedata.Timedata;

public interface DebugLoggable {

	// TODO should be merged with OpenemsComponent
	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

	/**
	 * Gets some output that is suitable for a continuous Debug log. Returns 'null'
	 * by default which causes no output.
	 *
	 * @return the debug log output
	 */
	public default String debugLog() {
		return null;
	}

	/**
	 * Gets some output that is suitable for a debug metrics to write down as
	 * {@link Timedata}.
	 * 
	 * @return the key value entries to write to write down; null or emptyMap for no
	 *         metrics
	 */
	public default Map<String, JsonElement> debugMetrics() {
		return emptyMap();
	}

}
