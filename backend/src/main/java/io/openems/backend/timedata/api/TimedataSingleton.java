package io.openems.backend.timedata.api;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public interface TimedataSingleton {
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
	public void write(String name, JsonObject jData);

	public JsonObject query(int _fems, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution/* , JsonObject kWh */) throws OpenemsException;
}
