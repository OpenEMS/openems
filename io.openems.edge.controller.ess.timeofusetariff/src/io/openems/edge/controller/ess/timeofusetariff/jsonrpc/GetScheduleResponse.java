package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalDouble;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.SUM_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.SUM_ESS_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.SUM_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyUtils.toPower;
import static java.lang.Math.round;
import static java.util.Optional.ofNullable;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.EshContext;
import io.openems.edge.controller.ess.timeofusetariff.Utils;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.Timedata;

/**
 * Represents a JSON-RPC Response for 'getMeters'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'schedule': [{
 *      'timestamp':...,
 *      'price':...,
 *      'state':...,
 *      'grid':...,
 *      'production':...,
 *      'consumption':...,
 *      'ess':...,
 *      'soc':...,
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
	 * @param componentId           the Component-ID of the parent
	 *                              {@link TimeOfUseTariffController}
	 * @param clock                 a {@link Clock}
	 * @param ess                   the {@link SymmetricEss}
	 * @param timedata              the {@link Timedata}
	 * @param energyScheduleHandler the {@link EnergyScheduleHandler}
	 * @return the {@link GetScheduleResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static GetScheduleResponse from(UUID requestId, String componentId, Clock clock, SymmetricEss ess,
			Timedata timedata,
			EnergyScheduleHandler.WithDifferentStates<StateMachine, EshContext> energyScheduleHandler) {
		final var schedule = energyScheduleHandler.getSchedule();
		final JsonArray result;
		if (schedule.isEmpty()) {
			result = new JsonArray();
		} else {
			final var historic = fromHistoricData(componentId, schedule.firstKey(), timedata);
			final var future = fromSchedule(ess, schedule);
			result = Stream.concat(historic, future) //
					.collect(toJsonArray());
		}

		return new GetScheduleResponse(requestId, //
				buildJsonObject() //
						.add("schedule", result) //
						.build());
	}

	/**
	 * Queries the last three hours' data and converts it to a {@link Stream} of
	 * {@link JsonObject}s suitable for a {@link GetScheduleResponse}.
	 * 
	 * @param componentId   Component-ID of {@link TimeOfUseTariffControllerImpl}
	 * @param firstSchedule {@link ZonedDateTime} of the first entry in the Schedule
	 *                      (rounded down to 15 minutes)
	 * @param timedata      the {@link Timedata}
	 * @return {@link Stream} of {@link JsonObject}s
	 */
	// TODO protected is sufficient after v1
	public static Stream<JsonObject> fromHistoricData(String componentId, ZonedDateTime firstSchedule,
			Timedata timedata) {
		// Process last three hours of historic data
		final var fromTime = firstSchedule.minusHours(3);
		final var toTime = firstSchedule.minusSeconds(1);
		final var channelQuarterlyPrices = new ChannelAddress(componentId, "QuarterlyPrices");
		final var channelStateMachine = new ChannelAddress(componentId, "StateMachine");
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = null;
		try {
			data = timedata.queryHistoricData(null, fromTime, toTime, //
					Set.of(channelQuarterlyPrices, channelStateMachine, //
							Utils.SUM_GRID, SUM_PRODUCTION, SUM_CONSUMPTION, SUM_ESS_DISCHARGE_POWER, SUM_ESS_SOC),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (Exception e) {
			LOG.warn("Unable to read historic data: " + e.getMessage());
		}
		if (data == null) {
			return Stream.of();
		}

		return data.entrySet().stream() //
				.map(e -> {
					var d = e.getValue();
					Function<ChannelAddress, JsonElement> getter = (c) -> ofNullable(d.get(c))
							.orElse(JsonNull.INSTANCE);

					return buildJsonObject() //
							.addProperty("timestamp", e.getKey()) //
							.addProperty("price",
									getAsOptionalDouble(getter.apply(channelQuarterlyPrices)).orElse(null)) //
							.addProperty("state",
									getAsOptionalInt(getter.apply(channelStateMachine)).orElse(BALANCING.getValue())) //
							.addProperty("grid", getAsOptionalInt(getter.apply(SUM_GRID)).orElse(null)) //
							.addProperty("production", getAsOptionalInt(getter.apply(SUM_PRODUCTION)).orElse(null)) //
							.addProperty("consumption", getAsOptionalInt(getter.apply(SUM_CONSUMPTION)).orElse(null)) //
							.addProperty("ess", getAsOptionalInt(getter.apply(SUM_ESS_DISCHARGE_POWER)).orElse(null)) //
							.addProperty("soc", getAsOptionalInt(getter.apply(SUM_ESS_SOC)).orElse(null)) //
							.build();
				});
	}

	/**
	 * Converts the Schedule to a {@link Stream} of {@link JsonObject}s suitable for
	 * a {@link GetScheduleResponse}.
	 * 
	 * @param ess      the {@link SymmetricEss}
	 * @param schedule the {@link EnergyScheduleHandler} schedule
	 * @return {@link Stream} of {@link JsonObject}s
	 */
	protected static Stream<JsonObject> fromSchedule(SymmetricEss ess,
			ImmutableSortedMap<ZonedDateTime, Period<StateMachine, EshContext>> schedule) {
		final var essTotalEnergy = ess.getCapacity().orElse(0);
		return schedule.entrySet().stream() //
				.map(e -> {
					var p = e.getValue();

					return buildJsonObject() //
							.addProperty("timestamp", e.getKey()) //
							.addProperty("price", p.price()) //
							.addProperty("state", p.state().getValue()) //
							.addProperty("grid", toPower(p.energyFlow().getGrid())) //
							.addProperty("production", toPower(p.energyFlow().getProd())) //
							.addProperty("consumption", toPower(p.energyFlow().getCons())) //
							.addProperty("ess", toPower(p.energyFlow().getEss())) //
							.addProperty("soc", round(fitWithin(0F, 100F, //
									p.essInitialEnergy() * 100F / essTotalEnergy))) //
							.build();
				});
	}

	/**
	 * Creates an empty default Schedule in case no Schedule is available.
	 * 
	 * @param clock        the {@link Clock}
	 * @param defaultState the default {@link StateMachine}
	 * @return {@link Stream} of {@link JsonObject}s
	 */
	protected static Stream<JsonObject> empty(Clock clock, StateMachine defaultState) {
		final var now = ZonedDateTime.now(clock);
		final var numberOfPeriods = 96;

		return IntStream.range(0, numberOfPeriods) //
				.mapToObj(i -> {
					return buildJsonObject() //
							.addProperty("timestamp", now.plusMinutes(i * 15)) //
							.add("price", JsonNull.INSTANCE) //
							.addProperty("state", defaultState.getValue()) //
							.add("grid", JsonNull.INSTANCE) //
							.add("production", JsonNull.INSTANCE) //
							.add("consumption", JsonNull.INSTANCE) //
							.add("ess", JsonNull.INSTANCE) //
							.add("soc", JsonNull.INSTANCE) //
							.build();
				});
	}

}
