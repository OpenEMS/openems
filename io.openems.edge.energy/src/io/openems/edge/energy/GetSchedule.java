package io.openems.edge.energy;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalDouble;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.AuthenticateWithToken;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.energy.GetSchedule.Request;
import io.openems.edge.energy.GetSchedule.Response;
import io.openems.edge.energy.api.handler.DifferentModes.Period.Transition;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.periods.PeriodData;
import io.openems.edge.energy.api.simulation.periods.PeriodData.Price;
import io.openems.edge.energy.optimizer.SimulationResult;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TariffManager;

/**
 * Gets the Schedule of today (history) and forecast.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "from": ZonedDateTime
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'data': [{
 *       ...
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetSchedule implements EndpointRequestType<Request, Response> {

	private static final int HISTORY_RESOLUTION = 5; // [minutes]
	private static final int PREDICTION_RESOLUTION = 15; // [minutes]

	@Override
	public String getMethod() {
		return "getSchedule";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			ZonedDateTime from //
	) {

		public Request {
			Objects.requireNonNull(from);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AuthenticateWithToken}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getZonedDateTime("from")), //
					obj -> buildJsonObject() //
							.addProperty("from", obj.from()) //
							.build());
		}

	}

	public record Response(//
			JsonArray data //
	) {

		/**
		 * Creates a new Response.
		 * 
		 * @param clock            the {@link Clock}
		 * @param request          the {@link Request}
		 * @param timedata         the {@link Timedata} service
		 * @param tariffManager    the {@link TariffManager} service
		 * @param predictorManager the {@link PredictorManager} service
		 * @param sr               the {@link SimulationResult}
		 * @return the created Response
		 */
		public static Response from(Request request, Clock clock, Timedata timedata, PredictorManager predictorManager,
				TariffManager tariffManager, SimulationResult sr) {
			final var from = request.from;
			final var to = from.plusHours(24);
			final var nowRaw = ZonedDateTime.now(clock);
			final var now = to.isBefore(nowRaw) //
					? DateUtils.roundDownToMinutes(ZonedDateTime.now(clock), HISTORY_RESOLUTION) //
					: DateUtils.roundDownToMinutes(ZonedDateTime.now(clock), PREDICTION_RESOLUTION);

			final Stream<Entry> history = now.isBefore(from) //
					? Stream.empty() //
					: Entries.fromHistory(from, now.isAfter(to) ? to : now, timedata, sr.schedules().keySet());
			final Stream<Entry> prediction = now.isAfter(to) //
					? Stream.empty() //
					: Entries.fromPrediction(now.isAfter(from) ? now : from, to, tariffManager, predictorManager, sr);

			return new Response(//
					Streams.concat(history, prediction) //
							.map(Entry::toJson) //
							.collect(toJsonArray()));
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetSchedule.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getJsonArray("data")), //
					obj -> buildJsonObject() //
							.add("data", obj.data()) //
							.build());
		}
	}

	private static record Entry(ZonedDateTime time, Type type, Sum sum, ImmutableList<Esh> eshs) {

		private enum Type {
			HISTORY, PREDICTION
		}

		private static record Sum(Double gridBuyPrice, Double gridSellPrice, Integer productionActivePower,
				Integer consumptionActivePower, Integer unmanagedConsumptionActivePower, Integer essDischargePower,
				Integer gridActivePower) {
			private JsonObject toJson() {
				return buildJsonObject() //
						.addPropertyIfNotNull("GridBuyPrice", this.gridBuyPrice) //
						.addPropertyIfNotNull("GridSellPrice", this.gridSellPrice) //
						.addPropertyIfNotNull("ProductionActivePower", this.productionActivePower) //
						.addPropertyIfNotNull("ConsumptionActivePower", this.consumptionActivePower) //
						.addPropertyIfNotNull("UnmanagedConsumptionActivePower", this.unmanagedConsumptionActivePower) //
						.addPropertyIfNotNull("EssDischargePower", this.essDischargePower) //
						.addPropertyIfNotNull("GridActivePower", this.gridActivePower) //
						.build();
			}
		}

		private JsonObject toJson() {
			return buildJsonObject() //
					.addProperty("timestamp", this.time) //
					.addProperty("type", this.type) //
					.add("_sum", this.sum.toJson()) //
					.add("eshs", this.eshs.stream() //
							.map(Esh::toJson) //
							.collect(toJsonArray())) //
					.build();
		}

		private static record Esh(String id, String mode, Integer managedConsumption) implements Comparable<Esh> {
			private JsonObject toJson() {
				return buildJsonObject() //
						.addProperty("id", this.id) //
						.addPropertyIfNotNull("mode", this.mode) //
						.addPropertyIfNotNull("managedConsumption", this.managedConsumption) //
						.build();
			}

			@Override
			public int compareTo(Esh o) {
				return ComparisonChain.start() //
						.compare(this.id, o.id) //
						.compare(this.mode, o.mode, nullsFirst(naturalOrder())) //
						.result();
			}
		}
	}

	protected static final ChannelAddress SUM_PRODUCTION = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());
	protected static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.CONSUMPTION_ACTIVE_POWER.id());
	protected static final ChannelAddress SUM_GRID = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.GRID_ACTIVE_POWER.id());
	protected static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER.id());
	protected static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.ESS_DISCHARGE_POWER.id());
	protected static final ChannelAddress SUM_GRID_BUY_PRICE = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.GRID_BUY_PRICE.id());
	protected static final ChannelAddress SUM_GRID_SELL_PRICE = new ChannelAddress(Sum.SINGLETON_COMPONENT_ID,
			Sum.ChannelId.GRID_SELL_PRICE.id());

	private static class Entries {

		private static final Logger LOG = LoggerFactory.getLogger(Entries.class);

		private static Stream<Entry> fromHistory(ZonedDateTime from, ZonedDateTime to, Timedata timedata,
				ImmutableSet<? extends EnergyScheduleHandler.WithDifferentModes> eshwdms) {
			final var channels = Streams //
					.concat(//
							Stream.of(SUM_GRID_BUY_PRICE, SUM_GRID_SELL_PRICE, SUM_GRID, SUM_PRODUCTION,
									SUM_CONSUMPTION, SUM_UNMANAGED_CONSUMPTION, SUM_ESS_DISCHARGE_POWER), //
							eshwdms.stream() //
									// TODO "StateMachine" is not a defined standard
									.map(esh -> new ChannelAddress(esh.getParentId(), "StateMachine"))) //
					.collect(toSet());

			// Query History
			final var data = queryHistoricData(timedata, from, to, channels,
					new Resolution(HISTORY_RESOLUTION, MINUTES));
			final var emptyMap = new TreeMap<ChannelAddress, JsonElement>();

			return Stream.iterate(from, t -> t.plus(HISTORY_RESOLUTION, MINUTES)) //
					.takeWhile(t -> t.isBefore(to)) //
					.map(t -> {
						final var d = data.getOrDefault(t.toInstant(), emptyMap);
						final Function<ChannelAddress, JsonElement> getter = c -> Optional.ofNullable(d.get(c))
								.orElse(JsonNull.INSTANCE);
						final Function<ChannelAddress, Double> getDouble = c -> getAsOptionalDouble(getter.apply(c))
								.orElse(null);
						final Function<ChannelAddress, Integer> getInteger = c -> getAsOptionalInt(getter.apply(c))
								.orElse(null);

						// Sum
						final var sum = new Entry.Sum(//
								getDouble.apply(SUM_GRID_BUY_PRICE), //
								getDouble.apply(SUM_GRID_SELL_PRICE), //
								getInteger.apply(SUM_PRODUCTION), //
								getInteger.apply(SUM_CONSUMPTION), //
								getInteger.apply(SUM_UNMANAGED_CONSUMPTION), //
								getInteger.apply(SUM_ESS_DISCHARGE_POWER), //
								getInteger.apply(SUM_GRID));

						// ESHs
						final var eshs = eshwdms.stream() //
								.map(esh -> {
									final var ctrlId = esh.getParentId();
									final var mode = getAsOptionalDouble(
											getter.apply(new ChannelAddress(ctrlId, "StateMachine")))
											.map(v -> esh.modes().get((int) Math.round(v))) //
											.map(Object::toString) //
											.orElse(null);
									// TODO mode value is just average; does not work for enums if value changed
									// during period
									// TODO query managed consumption if applicable
									return mode != null //
											? new Entry.Esh(ctrlId, mode, null) //
											: null;
								}) //
								.filter(Objects::nonNull) //
								.sorted() //
								.collect(toImmutableList());

						return new Entry(t, Entry.Type.HISTORY, sum, eshs);
					});
		}

		private static Stream<Entry> fromPrediction(ZonedDateTime from, ZonedDateTime to, TariffManager tariffManager,
				PredictorManager predictorManager, SimulationResult sr) {
			final var gridBuyPrices = tariffManager.getGridBuyDayAheadPrices();
			final var gridSellPrices = tariffManager.getGridSellDayAheadPrices();
			final var unmanagedConsumptionPrediction = predictorManager.getPrediction(SUM_UNMANAGED_CONSUMPTION);
			final var productionPrediction = predictorManager.getPrediction(SUM_PRODUCTION);

			// Convert to Instant-based Map
			final var data = sr.periods().entrySet().stream() //
					.collect(toSortedMap(//
							e -> e.getKey().toInstant(), //
							e -> e.getValue()));

			return Stream.iterate(from, t -> t.plus(PREDICTION_RESOLUTION, MINUTES)) //
					.takeWhile(t -> t.isBefore(to)) //
					.map(t -> {
						final var instant = t.toInstant();
						final var d = Optional.ofNullable(data.get(instant));
						final var period = d.map(p -> p.period());
						final var periodData = period.map(p -> p.data());
						final var ef = d.map(p -> p.energyFlow());
						final var duration = period.map(p -> p.duration());
						final IntUnaryOperator convertEnergyToPower = i -> duration.get().convertEnergyToPower(i);

						// Sum
						final var gridBuyPrice = periodData //
								.flatMap(PeriodData::gridBuyPrice) //
								.map(Price::actual) //
								.orElse(gridBuyPrices.getAt(instant));
						final var gridSellPrice = periodData //
								.flatMap(PeriodData::gridSellPrice) //
								.map(Price::actual) //
								.orElse(gridSellPrices.getAt(instant));
						final var production = ef //
								.map(EnergyFlow::getProduction) //
								.map(e -> convertEnergyToPower.applyAsInt(e)) //
								.orElse(productionPrediction.getAt(instant));
						final var unmanagedConsumption = ef //
								.map(EnergyFlow::getUnmanagedConsumption) //
								.map(e -> convertEnergyToPower.applyAsInt(e)) //
								.orElse(unmanagedConsumptionPrediction.getAt(instant));
						final var consumption = ef //
								.map(EnergyFlow::getConsumption) //
								.map(e -> convertEnergyToPower.applyAsInt(e)) //
								.orElse(unmanagedConsumption);
						final var ess = ef //
								.map(EnergyFlow::getEss) //
								.map(e -> convertEnergyToPower.applyAsInt(e)) //
								.orElse(null);
						final var grid = ef //
								.map(EnergyFlow::getGrid) //
								.map(e -> convertEnergyToPower.applyAsInt(e)) //
								.orElse(null);
						final var sum = new Entry.Sum(gridBuyPrice, gridSellPrice, production, consumption,
								unmanagedConsumption, ess, grid);

						// ESHs
						final var managedConsumptions = ef.map(EnergyFlow::getManagedConsumptions)
								.orElse(ImmutableSortedMap.of());
						final var eshs = sr.schedules().entrySet().stream() //
								.map(s -> {
									final var esh = s.getKey();
									final var id = esh.getParentId();
									final var schedule = s.getValue();
									final var managedConsumption = managedConsumptions.get(id);
									final var mode = Optional.ofNullable(schedule.get(t)) //
											.map(Transition::modeIndex) //
											.map(i -> esh.modes().getAsString(i)) //
											.orElse(null);
									return mode != null || managedConsumption != null //
											? new Entry.Esh(id, mode, managedConsumption) //
											: null;
								}) //
								.filter(Objects::nonNull) //
								.sorted() //
								.collect(toImmutableList());

						return new Entry(t, Entry.Type.PREDICTION, sum, eshs);
					});
		}

		private static SortedMap<Instant, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(Timedata timedata,
				ZonedDateTime from, ZonedDateTime to, Set<ChannelAddress> channels, Resolution resolution) {
			try {
				var data = timedata.queryHistoricData(null, from, to, channels, new Resolution(5, MINUTES));

				// Convert to Instant-based Map
				return data.entrySet().stream() //
						.collect(toSortedMap(//
								e -> e.getKey().toInstant(), //
								e -> e.getValue()));

			} catch (OpenemsNamedException e) {
				LOG.warn("Unable to read historic data: {}", e.getMessage(), e);
				return new TreeMap<>();
			}
		}
	}
}