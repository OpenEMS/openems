package io.openems.backend.timedata.api;

import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.common.api.TimedataSource;

public interface TimedataSingleton extends TimedataSource {
	/**
	 * Takes a JsonObject and writes the points to database.
	 *
	 * <pre>
	 * 	{
	 * 		"timestamp1" {
	 * 			"channel1": value,
	 * 			"channel2": value
	 * 		},
	 * 		"timestamp2" {
	 * 			"channel1": value,
	 * 			"channel2": value
	 *		}
	 *	}
	 * </pre>
	 */
	public void write(Optional<Integer> deviceId, JsonObject jData);
}
