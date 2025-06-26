package io.openems.backend.timedata.influx;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import io.openems.common.utils.DateUtils;

public class TimeFilter {

	private final long start; // [ms]
	private final long end; // [ms]

	/**
	 * Build a {@link TimeFilter} from String configurations.
	 * 
	 * @param startDate a string Start-Date in
	 *                  {@link DateTimeFormatter#ISO_LOCAL_DATE} format
	 * @param endDate   a string End-Date in
	 *                  {@link DateTimeFormatter#ISO_LOCAL_DATE} format
	 * @return a {@link TimeFilter}
	 */
	public static TimeFilter from(String startDate, String endDate) {
		final long start;
		{
			var tmp = DateUtils.parseLocalDateOrNull(startDate);
			if (tmp == null) {
				start = -1;
			} else {
				start = tmp.atStartOfDay(ZoneId.of("UTC")).toEpochSecond() * 1000;
			}
		}

		final long end;
		{
			var tmp = DateUtils.parseLocalDateOrNull(endDate);
			if (tmp == null) {
				end = -1;
			} else {
				end = tmp.atStartOfDay(ZoneId.of("UTC")).plusDays(1).toEpochSecond() * 1000;
			}
		}

		return new TimeFilter(start, end);
	}

	private TimeFilter(long start, long end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Tests the given {@link ZonedDateTime}s for validity.
	 * 
	 * @param times {@link ZonedDateTime}s to validate
	 * @return true if valid
	 */
	public boolean isValid(ZonedDateTime... times) {
		return Stream.of(times) //
				.allMatch(t -> this.isValid(t.toEpochSecond() * 1000));
	}

	/**
	 * Tests the given timestamp for validity.
	 * 
	 * @param timestampMs timestamp in milliseconds
	 * @return true if valid
	 */
	public boolean isValid(long timestampMs) {
		if (this.start != -1 && timestampMs < this.start) {
			return false;
		}
		if (this.end != -1 && timestampMs > this.end) {
			return false;
		}
		return true;
	}

}
