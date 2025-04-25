package io.openems.edge.energy.optimizer.app;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsZonedDateTime;
import static io.openems.common.utils.JsonUtils.getOptionalSubElement;
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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.energy.EnergySchedulerTestUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
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
		simulate(parseJson(json), executionLimitSeconds);
	}

	private static void simulate(GlobalOptimizationContext gsc, long executionLimitSeconds) {
		var simulator = new Simulator(gsc);

		var simulationResult = simulator.getBestSchedule(EMPTY_SIMULATION_RESULT, //
				false /* isCurrentPeriodFixed */, null, //
				stream -> stream //
						.limit(byExecutionTime(ofSeconds(executionLimitSeconds))));

		logSimulationResult(simulator, simulationResult);
	}

	private static GlobalOptimizationContext parseJson(JsonObject goc)
			throws IllegalArgumentException, OpenemsNamedException {
		final var startDateTime = getAsZonedDateTime(goc, "startTime");
		final var clock = new TimeLeapClock(startDateTime.toInstant(), ZoneId.of("UTC"));
		final var grid = GlobalOptimizationContext.Grid.fromJson(getAsJsonObject(goc, "grid"));
		final var ess = GlobalOptimizationContext.Ess.fromJson(getAsJsonObject(goc, "ess"));

		final var controllers = JsonUtils.stream(getAsJsonArray(goc, "eshs")) //
				.map(j -> {
					try {
						var parentFactoryPid = getAsString(j, "factoryPid");
						var parentId = getAsString(j, "id");
						var source = getOptionalSubElement(j, "source").orElse(JsonNull.INSTANCE);
						return EnergySchedulerTestUtils.createFromJson(parentFactoryPid, parentId, source);
					} catch (OpenemsNamedException e) {
						throw new IllegalArgumentException(e);
					}
				}) //
				.collect(toImmutableList());

		var nextTime = new AtomicReference<>(startDateTime);
		final var periods = JsonUtils.stream(JsonUtils.getAsJsonArray(goc, "periods")) //
				.map(p -> {
					try {
						var time = nextTime.get();
						var index = (int) Duration.between(startDateTime, time).toMinutes() / 15;
						nextTime.set(time.plusMinutes(15));
						return (GlobalOptimizationContext.Period) new GlobalOptimizationContext.Period.Quarter(index,
								time, //
								getAsInt(p, "production"), //
								getAsInt(p, "consumption"), //
								getAsDouble(p, "price"));
					} catch (OpenemsNamedException e) {
						throw new IllegalArgumentException(e);
					}
				}) //
				.collect(toImmutableList());

		var eshs = controllers.stream() //
				.map(EnergySchedulable::getEnergyScheduleHandler) //
				.collect(toImmutableList());

		return new GlobalOptimizationContext(clock, RiskLevel.MEDIUM, startDateTime, //
				eshs, filterEshsWithDifferentModes(eshs).collect(toImmutableList()), //
				grid, ess, periods);
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
