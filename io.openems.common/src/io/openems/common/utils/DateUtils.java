package io.openems.common.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.OpenemsException;

public class DateUtils {

	private DateUtils() {
	}

	/**
	 * Asserts that both dates are in the same timezone.
	 *
	 * @param date1 the first Date
	 * @param date2 the second Date
	 * @throws OpenemsException if dates are not in the same timezone
	 */
	public static void assertSameTimezone(ZonedDateTime date1, ZonedDateTime date2) throws OpenemsException {
		if (ZoneOffset.from(date1).getTotalSeconds() != ZoneOffset.from(date2).getTotalSeconds()) {
			throw new OpenemsException("FromDate and ToDate need to be in the same timezone!");
		}
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to given minutes.
	 *
	 * @param d       the {@link ZonedDateTime}
	 * @param minutes the minutes to round down to; max 59
	 * @return the rounded result
	 */
	public static ZonedDateTime roundZonedDateTimeDownToMinutes(ZonedDateTime d, int minutes) {
		return d.withMinute(d.getMinute() - d.getMinute() % minutes).truncatedTo(ChronoUnit.MINUTES);
	}
}
