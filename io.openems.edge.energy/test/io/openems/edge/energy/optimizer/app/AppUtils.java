package io.openems.edge.energy.optimizer.app;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period;
import io.openems.edge.energy.optimizer.SimulationResult;

public class AppUtils {

	private AppUtils() {
	}

	/**
	 * Creates {@link GlobalSimulationsContext} from the log output of
	 * {@link SimulationResult#toLogString()}.
	 * 
	 * @param log  the log output of {@link SimulationResult#toLogString()}
	 * @param eshs the {@link EnergyScheduleHandler}s
	 * @return a {@link GlobalSimulationsContext}
	 */
	public static GlobalSimulationsContext parseGlobalSimulationsContextFromLogString(String log,
			ImmutableList<EnergyScheduleHandler> eshs) throws IllegalArgumentException {
		var headerMatcher = log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && l.contains("GlobalSimulationsContext")) //
				.map(l -> applyPattern(HEADER_PATTERN, l)) //
				.findFirst().get();
		final var startDateTime = ZonedDateTime.parse(headerMatcher.group("startTime"));
		final var clock = new TimeLeapClock(startDateTime.toInstant(), ZoneId.of("UTC"));

		var gridMatcher = applyPattern(GRID_PATTERN, headerMatcher.group("grid"));
		final var grid = new GlobalSimulationsContext.Grid(//
				parseInt(gridMatcher.group("maxBuy")), //
				parseInt(gridMatcher.group("maxSell")));

		var essMatcher = applyPattern(ESS_PATTERN, headerMatcher.group("ess"));
		final var ess = new GlobalSimulationsContext.Ess(//
				parseInt(essMatcher.group("currentEnergy")), //
				parseInt(essMatcher.group("totalEnergy")), //
				parseInt(essMatcher.group("maxChargeEnergy")), //
				parseInt(essMatcher.group("maxDischargeEnergy")));

		var nextTime = new AtomicReference<>(startDateTime);
		var periods = log.lines() //
				.filter(l -> l.contains("OPTIMIZER ") && !l.contains("GlobalSimulationsContext") && !l.contains("Time")) //
				.map(l -> applyPattern(PERIOD_PATTERN, l)) //
				.map(m -> {
					var time = nextTime.get();
					if (!nextTime.get().toLocalTime().equals(LocalTime.parse(m.group("time"), HOURS_MINUTES))) {
						throw new IllegalArgumentException("Times do not match: " + time);
					}
					nextTime.set(time.plusMinutes(15));
					return (Period) new Period.Quarter(time, //
							parseInt(m.group("production")), //
							parseInt(m.group("consumption")), //
							parseDouble(m.group("price")));
				}) //
				.collect(toImmutableList());

		return new GlobalSimulationsContext(clock, new AtomicInteger(), startDateTime, eshs, grid, ess, periods);
	}

	private static Matcher applyPattern(Pattern pattern, String line) {
		var matcher = pattern.matcher(line);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Pattern does not match");
		}
		return matcher;
	}

	private static final DateTimeFormatter HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm");

	private static final Pattern HEADER_PATTERN = Pattern.compile("" //
			+ "startTime=(?<startTime>\\S*), " //
			+ "grid=(?<grid>.*), " //
			+ "ess=(?<ess>.*)");

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

}
