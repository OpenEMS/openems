package io.openems.backend.timedata.api;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.device.MetadataDevice;
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
	public void write(MetadataDevice device, JsonObject jData);
}
