package io.openems.edge.energy.optimizer.app;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.common.type.RegexUtils.applyPatternOrError;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.time.Duration.ofSeconds;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonElementPathActual.JsonElementPathActualNonNull;
import io.openems.common.jsonrpc.serialization.JsonObjectPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.energy.EnergySchedulerTestUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Ess;
import io.openems.edge.energy.optimizer.SimulationResult;
import io.openems.edge.energy.optimizer.Simulator;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public final class AppUtils {

	private AppUtils() {
	}

	protected static void simulateFromLog(String log, long executionLimitSeconds)
			throws IllegalArgumentException, OpenemsNamedException {
		simulateFromJson(parseLog(log), executionLimitSeconds);
	}

	protected static void simulateFromJson(JsonObject json, long executionLimitSeconds)
			throws IllegalArgumentException, OpenemsNamedException {
		simulate(globalOptimizationContextSerializer().deserialize(json), executionLimitSeconds);
	}

	private static void simulate(GlobalOptimizationContext gsc, long executionLimitSeconds) {
		var simulator = new Simulator(gsc);

		var simulationResult = simulator.getBestSchedule(EMPTY_SIMULATION_RESULT, //
				false /* isCurrentPeriodFixed */, null, //
				stream -> stream //
						.limit(byExecutionTime(ofSeconds(executionLimitSeconds))));

		logSimulationResult(simulator, simulationResult);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link GlobalOptimizationContext}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<GlobalOptimizationContext> globalOptimizationContextSerializer() {
		return jsonObjectSerializer(GlobalOptimizationContext.class, json -> {
			final var startTime = json.getZonedDateTime("startTime");
			final var clock = new TimeLeapClock(startTime.toInstant(), ZoneId.of("UTC"));
			final var componentManager = new DummyComponentManager(clock);
			final var ess = json.getObject("ess", Ess.serializer());
			final var sum = new DummySum() //
					.withEssSoc(ess.currentEnergy() * 100 / ess.totalEnergy()) //
					.withEssCapacity(ess.totalEnergy());

			// Periods: Predictions and Prices
			final TimeOfUseTariff timeOfUseTariff;
			final PredictorManager predictorManager;
			try {
				final var prices = ImmutableSortedMap.<Instant, Double>naturalOrder();
				final var productions = ImmutableSortedMap.<Instant, Integer>naturalOrder();
				final var consumptions = ImmutableSortedMap.<Instant, Integer>naturalOrder();
				final var timeParser = new TimeParser(startTime);
				json.getJsonArray("periods").forEach(e -> {
					var p = new JsonElementPathActualNonNull(e).getAsJsonObjectPath();
					var time = timeParser.apply(p);
					p.getNullableNumberPath("price").getAsOptionalDouble() //
							.ifPresent(price -> prices.put(time, price));
					p.getNullableNumberPath("production").getAsOptionalInt() //
							.ifPresent(production -> productions.put(time, production));
					p.getNullableNumberPath("consumption").getAsOptionalInt() //
							.ifPresent(consumption -> consumptions.put(time, consumption));
				});
				timeOfUseTariff = new DummyTimeOfUseTariffProvider(clock, TimeOfUsePrices.from(prices.build()));
				predictorManager = new DummyPredictorManager(//
						new DummyPredictor("predictor0", componentManager, //
								Prediction.from(productions.build()), SUM_PRODUCTION),
						new DummyPredictor("predictor1", componentManager, //
								Prediction.from(consumptions.build()), SUM_UNMANAGED_CONSUMPTION));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e.getMessage());
			}

			final var controllers = json.getJsonArrayPath("eshs").getAsImmutableList(e -> {
				var j = e.getAsJsonObjectPath();
				var parentFactoryPid = j.getString("factoryPid");
				var parentId = j.getString("id");
				var source = j.getNullableJsonObjectPath("source").getOrNull();
				return EnergySchedulerTestUtils.createFromJson(parentFactoryPid, parentId, source);
			});

			final var eshs = controllers.stream() //
					.map(EnergySchedulable::getEnergyScheduleHandler) //
					.collect(toImmutableList());

			return GlobalOptimizationContext.create() //
					.setComponentManager(componentManager) //
					.setRiskLevel(json.getEnum("riskLevel", RiskLevel.class)) //
					.setEnergyScheduleHandlers(eshs) //
					.setSum(sum) //
					.setPredictorManager(predictorManager) //
					.setTimeOfUseTariff(timeOfUseTariff) //
					.build();

		}, GlobalOptimizationContext::toJson);
	}

	/**
	 * Parses the log output of {@link SimulationResult#toLogString()} to a
	 * {@link JsonObject}.
	 * 
	 * @param log the log output of {@link SimulationResult#toLogString()}
	 * @return a {@link JsonObject}
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private static JsonObject parseLog(String log) throws IllegalArgumentException, OpenemsNamedException {
		var header = parseToJsonObject(log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && l.contains("GlobalOptimizationContext")) //
				.map(l -> applyPatternOrError(HEADER_PATTERN, l)) //
				.map(m -> m.group("json")) //
				.findFirst().get());
		var goc = getAsJsonObject(header, "GlobalOptimizationContext");
		goc.add("periods", log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && !l.contains("GlobalOptimizationContext")
						&& !l.contains("Time")) //
				.map(l -> applyPatternOrError(PERIOD_PATTERN, l)) //
				.map(m -> buildJsonObject() //
						.addProperty("time", m.group("time")) //
						.addProperty("price", m.group("price").equals("-") //
								? null //
								: Double.parseDouble(m.group("price"))) //
						.addProperty("production", Integer.parseInt(m.group("production"))) //
						.addProperty("consumption", Integer.parseInt(m.group("consumption"))) //
						.build()) //
				.collect(toJsonArray()));
		return goc;
	}

	private static final Pattern HEADER_PATTERN = Pattern.compile("" //
			+ "OPTIMIZER (?<json>\\{\\\"GlobalOptimizationContext\\\".*\\}$)");

	private static final Pattern PERIOD_PATTERN = Pattern.compile("" //
			+ "(?<time>\\d{2}:\\d{2})" //
			+ "\\s+(?<price>-?\\d*)" //
			+ "\\s+(?<production>-?\\d+)" //
			+ "\\s+(?<consumption>-?\\d+)");

	protected static JsonElement period(String time, double production, double consumption, double price) {
		return buildJsonObject() //
				.addProperty("time", time) //
				.addProperty("production", production) //
				.addProperty("consumption", consumption) //
				.addProperty("price", price) //
				.build();
	}

	private static class TimeParser implements Function<JsonObjectPath, Instant> {

		private final ZonedDateTime start;

		private ZonedDateTime last = null;

		public TimeParser(ZonedDateTime start) {
			this.start = start;
		}

		@Override
		public synchronized Instant apply(JsonObjectPath p) {
			var time = p.getLocalTime("time");
			var base = this.last == null //
					? this.start //
					: this.last;
			var result = base.with(time);
			if (result.isBefore(base)) {
				result = result.plusDays(1);
			}
			this.last = result;
			return result.toInstant();
		}
	}
}
