package io.openems.edge.controller.ess.timeofusetariff;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

public class ScheduleUtils {

	/**
	 * Generates a 24-hour schedule as a {@link JsonArray} based on input data for
	 * prices and states. The resulting schedule includes timestamped entries for
	 * each 15-minute interval. and 'states' data.
	 * 
	 * @param prices    The quarterly prices for 24 hours (3-hour past, 21-hour
	 *                  future).
	 * @param states    The states for 24 hours (3-hour past, 21-hour future).
	 * @param timeStamp The time stamp of the first entry in the schedule.
	 * @return The schedule data as a {@link JsonArray}.
	 */
	public static JsonArray createSchedule(JsonArray prices, JsonArray states, ZonedDateTime timeStamp) {

		var schedule = JsonUtils.buildJsonArray();

		// Creates the Json object with 'time stamp', 'price', 'state' and add them to
		// the Json array.
		for (int index = 0; index < prices.size(); index++) {
			var result = JsonUtils.buildJsonObject();
			var price = prices.get(index);
			var state = states.get(index);

			if (price.isJsonNull() || state.isJsonNull()) {
				continue;
			}

			// Calculate the timestamp for the current entry, adding 15 minutes for each
			// index.
			var entryTimeStamp = timeStamp.plusMinutes(15 * index).format(DateTimeFormatter.ISO_INSTANT);

			result.add("timestamp", new JsonPrimitive(entryTimeStamp));
			result.add("price", price);
			result.add("state", state);

			// Add the JSON object to the schedule array.
			schedule.add(result.build());
		}

		return schedule.build();
	}
}
