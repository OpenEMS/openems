package io.openems.edge.controller.evcs.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.common.utils.StringUtils.toShortString;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.controller.evcs.SmartMode;
import io.openems.edge.controller.evcs.Utils.EshContext.EshSmartContext;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period;

/**
 * Represents a JSON-RPC Response for 'getSchedule'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'schedule': [{
 *      'timestamp':...,
 *      'price':...,
 *      'state':...
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetScheduleResponse extends JsonrpcResponseSuccess {

	private static final Logger LOG = LoggerFactory.getLogger(GetScheduleResponse.class);

	private final JsonObject result;

	public GetScheduleResponse(UUID id, JsonObject result) {
		super(id);
		this.result = result;
	}

	@Override
	public JsonObject getResult() {
		return this.result;
	}

	/**
	 * Builds a {@link GetScheduleResponse} with last three hours data and current
	 * Schedule.
	 * 
	 * @param requestId             the JSON-RPC request-id
	 * @param energyScheduleHandler the {@link EnergyScheduleHandler}
	 * @return the {@link GetScheduleResponse}
	 */
	public static GetScheduleResponse from(UUID requestId, EnergyScheduleHandler energyScheduleHandler) {
		LOG.info("OPTIMIZER JSONRPC start");
		if (energyScheduleHandler == null
				|| energyScheduleHandler instanceof EnergyScheduleHandler.WithOnlyOneState<?>) {
			return null;
		}
		@SuppressWarnings("unchecked")
		var esh = (EnergyScheduleHandler.WithDifferentStates<SmartMode, EshSmartContext>) energyScheduleHandler;
		final var schedule = esh.getSchedule();
		final JsonArray result;
		if (schedule.isEmpty()) {
			result = new JsonArray();
		} else {
			final var historic = Stream.<JsonObject>of(); // TODO
			final var future = fromSchedule(schedule);
			result = Stream.concat(historic, future) //
					.collect(toJsonArray());
		}
		LOG.info("OPTIMIZER JSONRPC finished. " + toShortString(result, 100));

		return new GetScheduleResponse(requestId, //
				buildJsonObject() //
						.add("schedule", result) //
						.build());
	}

	/**
	 * Converts the Schedule to a {@link Stream} of {@link JsonObject}s suitable for
	 * a {@link GetScheduleResponse}.
	 * 
	 * @param schedule the {@link EnergyScheduleHandler} schedule
	 * @return {@link Stream} of {@link JsonObject}s
	 */
	protected static Stream<JsonObject> fromSchedule(
			ImmutableSortedMap<ZonedDateTime, Period<SmartMode, EshSmartContext>> schedule) {
		return schedule.entrySet().stream() //
				.map(e -> {
					var p = e.getValue();

					return buildJsonObject() //
							.addProperty("timestamp", e.getKey()) //
							.addProperty("price", p.price()) //
							.addProperty("state", p.state().getValue()) //
							.build();
				});
	}

	/**
	 * Creates an empty default Schedule in case no Schedule is available.
	 * 
	 * @param clock       the {@link Clock}
	 * @param defaultMode the default {@link SmartMode}
	 * @return {@link Stream} of {@link JsonObject}s
	 */
	protected static Stream<JsonObject> empty(Clock clock, SmartMode defaultMode) {
		final var now = ZonedDateTime.now(clock);
		final var numberOfPeriods = 96;

		return IntStream.range(0, numberOfPeriods) //
				.mapToObj(i -> {
					return buildJsonObject() //
							.addProperty("timestamp", now.plusMinutes(i * 15)) //
							.add("price", JsonNull.INSTANCE) //
							.addProperty("state", defaultMode.getValue()) //
							.build();
				});
	}

}
