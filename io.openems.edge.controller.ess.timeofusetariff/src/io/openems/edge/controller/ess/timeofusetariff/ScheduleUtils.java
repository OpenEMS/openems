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

	public static JsonArray getSchedule(JsonArray prices, JsonArray states, ZonedDateTime timeStamp) {

		var schedule = JsonUtils.buildJsonArray();

		// Creates the Json object with 'time stamp', 'price', 'state' and adds it to
		// the Json array.
		for (int index = 0; index < prices.size(); index++) {
			var result = JsonUtils.buildJsonObject();
			var price = prices.get(index);
			var state = states.get(index);

			if (price.isJsonNull() || state.isJsonNull()) {
				continue;
			}

			result.add("timestamp",
					new JsonPrimitive(timeStamp.plusMinutes(15 * index).format(DateTimeFormatter.ISO_INSTANT)));
			result.add("price", price);
			result.add("state", state);
			schedule.add(result.build());
		}

		return schedule.build();
	}

	/**
	 * 
	 * @param schedule
	 * @param config
	 * @param request
	 * @param queryResult
	 * @param channeladdressPrices
	 * @param channeladdressStateMachine
	 * @return
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
