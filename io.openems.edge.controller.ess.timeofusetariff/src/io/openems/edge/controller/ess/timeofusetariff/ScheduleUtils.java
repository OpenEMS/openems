package io.openems.edge.controller.ess.timeofusetariff;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleRequest;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;

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
	public static JsonArray getSchedule(JsonArray prices, JsonArray states, ZonedDateTime timeStamp) {

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

	/**
	 * Utilizes the previous three hours' data and computes the next 21 hours data
	 * from the {@link Schedule} provided, then concatenates them to generate a
	 * 24-hour {@link Schedule}.
	 * 
	 * @param schedule                   The {@link Schedule}.
	 * @param config                     The {@link Config}.
	 * @param request                    The {@link GetScheduleRequest}.
	 * @param queryResult                The historic data.
	 * @param channeladdressPrices       The {@link ChannelAddress} for Quarterly
	 *                                   prices.
	 * @param channeladdressStateMachine The {@link ChannelAddress} for the state
	 *                                   machine.
	 * @return The {@link GetScheduleResponse}.
	 */
	public static GetScheduleResponse handleGetScheduleRequest(Schedule schedule, Config config,
			GetScheduleRequest request, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult,
			ChannelAddress channeladdressPrices, ChannelAddress channeladdressStateMachine) {

		// Extract the price data
		var priceValuesPast = queryResult.values().stream() //
				// Only specific channel address values.
				.map(t -> t.get(channeladdressPrices)) //
				// get as Array
				.collect(JsonUtils.toJsonArray());

		// Extract the State Machine data
		var stateMachineValuesPast = queryResult.values().stream() //
				// Only specific channel address values.
				.map(t -> t.get(channeladdressStateMachine)) //
				// Mapping to absolute state machine values since query result gives average
				// values.
				.map(t -> {
					if (t.isJsonPrimitive() && t.getAsJsonPrimitive().isNumber()) {
						// 'double' to 'int' for appropriate state machine values.
						return new JsonPrimitive(t.getAsInt());
					}

					return JsonNull.INSTANCE;
				})
				// get as Array
				.collect(JsonUtils.toJsonArray());

		final var stateMachineValuesFuture = new JsonArray();
		final var priceValuesFuture = new JsonArray();

		// Create StateMachine for future values based on schedule created.
		schedule.periods.forEach(period -> {
			priceValuesFuture.add(period.price);
			stateMachineValuesFuture.add(period.getStateMachine(config.controlMode()).getValue());
		});

		var prices = Stream.concat(//
				JsonUtils.stream(priceValuesPast), // Last 3 hours data.
				JsonUtils.stream(priceValuesFuture)) // Next 21 hours data.
				.limit(96) //
				.collect(JsonUtils.toJsonArray());

		var states = Stream.concat(//
				JsonUtils.stream(stateMachineValuesPast), // Last 3 hours data
				JsonUtils.stream(stateMachineValuesFuture)) // Next 21 hours data.
				.limit(96) //
				.collect(JsonUtils.toJsonArray());

		var timestamp = queryResult.firstKey();
		var result = ScheduleUtils.getSchedule(prices, states, timestamp);

		return new GetScheduleResponse(request.getId(), result);
	}
}
