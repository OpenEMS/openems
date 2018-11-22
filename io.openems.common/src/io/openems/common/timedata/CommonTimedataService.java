package io.openems.common.timedata;

import java.time.ZonedDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public interface CommonTimedataService {

	/**
	 * Queries the database and returns a JsonArray of the form
	 *
	 * <pre>
	 *	[{
	 *  	timestamp: "2017-03-21T08:55:20Z",
	 *  	channels: {
	 *			'thing': {
	 *				'channel': 'value'
	 *			}
	 *		}
	 * 	}]
	 * </pre>
	 */
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, Tag... tags) throws OpenemsException;

	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution,
			boolean cumulative, Tag[] tags) throws OpenemsException;

}
