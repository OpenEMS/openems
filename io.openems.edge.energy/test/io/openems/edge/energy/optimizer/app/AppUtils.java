package io.openems.edge.energy.optimizer.app;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.common.type.RegexUtils.applyPatternOrError;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.time.Duration.ofSeconds;

import java.time.Duration;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.EnergySchedulerTestUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Ess;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Grid;
import io.openems.edge.energy.optimizer.SimulationResult;
import io.openems.edge.energy.optimizer.Simulator;

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
			final var controllers = json.getJsonArrayPath("eshs").getAsImmutableList(e -> {
				var j = e.getAsJsonObjectPath();
				var parentFactoryPid = j.getString("factoryPid");
				var parentId = j.getString("id");
				var source = j.getNullableJsonObjectPath("source").getOrNull();
				return EnergySchedulerTestUtils.createFromJson(parentFactoryPid, parentId, source);
			});

			var nextTime = new AtomicReference<>(startTime);
			final var periods = json.getJsonArrayPath("periods").getAsImmutableList(e -> {
				var j = e.getAsJsonObjectPath();
				var time = nextTime.get();
				var index = (int) Duration.between(startTime, time).toMinutes() / 15;
				nextTime.set(time.plusMinutes(15));
				return (GlobalOptimizationContext.Period) GlobalOptimizationContext.Period.Quarter.from(//
						index, time, //
						j.getInt("production"), j.getInt("consumption"), j.getDouble("price"));
			});

			final var eshs = controllers.stream() //
					.map(EnergySchedulable::getEnergyScheduleHandler) //
					.collect(toImmutableList());
			var eshsWithDifferentModes = filterEshsWithDifferentModes(eshs) //
					.collect(toImmutableList());

			return new GlobalOptimizationContext(//
					clock, //
					json.getEnum("riskLevel", RiskLevel.class), //
					startTime, //
					eshs, //
					eshsWithDifferentModes, //
					json.getObject("grid", Grid.serializer()), //
					json.getObject("ess", Ess.serializer()), //
					periods); //

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
						.addProperty("production", Integer.parseInt(m.group("production"))) //
						.addProperty("consumption", Integer.parseInt(m.group("consumption"))) //
						.addProperty("price", Double.parseDouble(m.group("price"))) //
						.build()) //
				.collect(toJsonArray()));
		return goc;
	}

	private static final Pattern HEADER_PATTERN = Pattern.compile("" //
			+ "OPTIMIZER (?<json>\\{\\\"GlobalOptimizationContext\\\".*\\}$)");

	private static final Pattern PERIOD_PATTERN = Pattern.compile("" //
			+ "(?<time>\\d{2}:\\d{2})" //
			+ "\\s+(?<price>-?\\d+)" //
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
}
