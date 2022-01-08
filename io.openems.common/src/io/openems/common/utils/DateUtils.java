package io.openems.common.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

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
}
