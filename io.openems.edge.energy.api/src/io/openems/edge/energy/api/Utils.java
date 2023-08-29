package io.openems.edge.energy.api;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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
	public static ZonedDateTime toZonedDateTime(ZonedDateTime now, int hour) {
		var result = now.truncatedTo(ChronoUnit.HOURS).withHour(hour);
		if (now.getHour() <= hour) {
			return result;
		} else {
			return result.plusDays(1);
		}
	}
}
