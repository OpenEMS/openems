package io.openems.edge.energy.optimizer.app;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.optimizer.SimulationResult;

public class AppUtils {

	private AppUtils() {
	}

	/**
	 * Creates {@link GlobalOptimizationContext} from the log output of
	 * {@link SimulationResult#toLogString()}.
	 * 
	 * @param log  the log output of {@link SimulationResult#toLogString()}
	 * @param eshs the {@link EnergyScheduleHandler}s
	 * @return a {@link GlobalOptimizationContext}
	 */
	public static GlobalOptimizationContext parseGlobalOptimizationContextFromLogString(String log,
			ImmutableList<EnergyScheduleHandler> eshs) throws IllegalArgumentException {
		var headerMatcher = log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && l.contains("GlobalOptimizationContext")) //
				.map(l -> applyPattern(HEADER_PATTERN, l)) //
				.findFirst().get();
		final var startDateTime = ZonedDateTime.parse(headerMatcher.group("startTime"));
		final var clock = new TimeLeapClock(startDateTime.toInstant(), ZoneId.of("UTC"));

		var gridMatcher = applyPattern(GRID_PATTERN, headerMatcher.group("grid"));
		final var grid = new GlobalOptimizationContext.Grid(//
				parseInt(gridMatcher.group("maxBuy")), //
				parseInt(gridMatcher.group("maxSell")));

		var essMatcher = applyPattern(ESS_PATTERN, headerMatcher.group("ess"));
		final var ess = new GlobalOptimizationContext.Ess(//
				parseInt(essMatcher.group("currentEnergy")), //
				parseInt(essMatcher.group("totalEnergy")), //
				parseInt(essMatcher.group("maxChargeEnergy")), //
				parseInt(essMatcher.group("maxDischargeEnergy")));

		applyPattern(ESHS_PATTERN, headerMatcher.group("eshs")).results() //
				.forEach(eshMatcher -> {
					// TODO use for ControllerOptimizationContext
					System.out.println(eshMatcher.group("id") + ": " + eshMatcher.group("coc"));
				});

		var nextTime = new AtomicReference<>(startDateTime);
		var periods = log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && !l.contains("GlobalOptimizationContext")
						&& !l.contains("Time")) //
				.map(l -> applyPattern(PERIOD_PATTERN, l)) //
				.map(m -> {
					var time = nextTime.get();
					if (!nextTime.get().toLocalTime().equals(LocalTime.parse(m.group("time"), HOURS_MINUTES))) {
						throw new IllegalArgumentException("Times do not match: " + time);
					}
					var index = (int) Duration.between(startDateTime, time).toMinutes() / 15;
					nextTime.set(time.plusMinutes(15));
					return (GlobalOptimizationContext.Period) new GlobalOptimizationContext.Period.Quarter(index, time, //
							parseInt(m.group("production")), //
							parseInt(m.group("consumption")), //
							parseDouble(m.group("price")));
				}) //
				.collect(toImmutableList());

		return new GlobalOptimizationContext(clock, RiskLevel.MEDIUM, startDateTime, //
				eshs, filterEshsWithDifferentModes(eshs).collect(toImmutableList()), //
				grid, ess, periods);
	}

	private static Matcher applyPattern(Pattern pattern, String line) {
		var matcher = pattern.matcher(line);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Pattern [" + pattern + "] does not match line [" + line + "]");
		}
		return matcher;
	}

	private static final DateTimeFormatter HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm");

	private static final Pattern HEADER_PATTERN = Pattern.compile("" //
			+ "startTime=(?<startTime>\\S*), " //
			+ "Grid\\[(?<grid>.*)\\], " //
			+ "Ess\\[(?<ess>.*)\\], " //
			+ "eshs=\\[(?<eshs>.*)\\]" //
			+ "}");

	private static final Pattern GRID_PATTERN = Pattern.compile("" //
			+ "maxBuy=(?<maxBuy>\\d+), " //
			+ "maxSell=(?<maxSell>\\d+)");

	private static final Pattern ESS_PATTERN = Pattern.compile("" //
			+ "currentEnergy=(?<currentEnergy>\\d+), " //
			+ "totalEnergy=(?<totalEnergy>\\d+), " //
			+ "maxChargeEnergy=(?<maxChargeEnergy>\\d+), " //
			+ "maxDischargeEnergy=(?<maxDischargeEnergy>\\d+)");

	private static final Pattern PERIOD_PATTERN = Pattern.compile("" //
			+ "(?<time>\\d{2}:\\d{2})" //
			+ "\\s+(?<price>-?\\d+\\.\\d+)" //
			+ "\\s+(?<production>-?\\d+)" //
			+ "\\s+(?<consumption>-?\\d+)");

	private static final Pattern ESHS_PATTERN = Pattern.compile("ESH\\.(?<id>\\S+?)\\{(?<coc>.*?)\\}");
}
