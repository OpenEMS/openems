package io.openems.edge.energy.api;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// TODO Methods are obsolete at the moment
public final class Utils {

	private Utils() {
	}

	/**
	 * Gets the {@link ZonedDateTime} truncated to future hour.
	 * 
	 * @param now  input {@link ZonedDateTime}
	 * @param hour the hour
	 * @return truncated {@link ZonedDateTime}
	 */
	public static ZonedDateTime toFutureHour(ZonedDateTime now, int hour) {
		var result = now.truncatedTo(ChronoUnit.HOURS).withHour(hour);
		if (now.getHour() <= hour) {
			return result;
		} else {
			return result.plusDays(1);
		}
	}

	/**
	 * Gets the {@link ZonedDateTime} truncated to future quarter.
	 * 
	 * @param now     input {@link ZonedDateTime}
	 * @param quarter the quarter
	 * @return truncated {@link ZonedDateTime}
	 */
	public static ZonedDateTime toFutureQuarter(ZonedDateTime now, int quarter) {
		var hour = quarter / 4;
		var result = now.truncatedTo(ChronoUnit.HOURS).withHour(hour).withMinute((quarter % 4) * 15);
		if (now.getHour() <= hour) {
			return result;
		} else {
			return result.plusDays(1);
		}
	}
}
