package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public record Period(ZonedDateTime time, int production, int consumption, int essInitial, StateMachine state,
		int essChargeDischarge, int grid, double price, double cost) {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * Gets the {@link Period#toString()} header.
	 * 
	 * @return header
	 */
	public static String header() {
		return "Time  Production Consumption EssInitial State             EssChargeDischarge  Grid Price  Cost";
	}

	/**
	 * Gets the Period as String.
	 */
	public String toString() {
		return String.format(Locale.ENGLISH, "%s %10d %11d %10d %-17s %18d %5d %.2f %.4f", //
				this.time.format(FORMATTER), this.production, this.consumption, //
				this.essInitial, //
				this.state, //
				this.essChargeDischarge, this.grid, this.price, this.cost);
	}

	private static final Pattern PATTERN = Pattern.compile("" //
			+ "(?<time>\\d{2}:\\d{2})" //
			+ "\\s+(?<production>-?\\d+)" //
			+ "\\s+(?<consumption>-?\\d+)" //
			+ "\\s+(?<essInitial>-?\\d+)" //
			+ "\\s+(?<state>-?\\w+)" //
			+ "\\s+(?<essChargeDischarge>-?\\d+)" //
			+ "\\s+(?<grid>-?\\d+)" //
			+ "\\s+(?<price>-?\\d+\\.\\d*)" //
			+ "\\s+(?<cost>-?\\d+\\.\\d*)$");

	/**
	 * Gets the Period from the Log String.
	 * 
	 * <p>
	 * This is the reverse of {@link #toString()} method.
	 * 
	 * @return a new Period
	 * @throws RuntimeException on error
	 */
	public static Period fromLog(String log) throws RuntimeException {
		var matcher = PATTERN.matcher(log);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Pattern does not match");
		}
		return new Period(//
				LocalTime.parse(matcher.group("time"), FORMATTER).atDate(LocalDate.MIN).atZone(ZoneId.of("UTC")), //
				Integer.parseInt(matcher.group("production")), //
				Integer.parseInt(matcher.group("consumption")), //
				Integer.parseInt(matcher.group("essInitial")), //
				StateMachine.valueOf(matcher.group("state")), //
				Integer.parseInt(matcher.group("essChargeDischarge")), //
				Integer.parseInt(matcher.group("grid")), //
				Double.parseDouble(matcher.group("price")), //
				Double.parseDouble(matcher.group("cost")));
	}
}