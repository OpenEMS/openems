package io.openems.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.DurationUnit;

public class DateUtils {

	/**
	 * Day-Month-Year with dots separated {@link DateTimeFormatter}.
	 */
	public static final DateTimeFormatter DMY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	/**
	 * {@link DateTimeFormatter} with inclusively 24:00 which is converted to 00:00.
	 */
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private DateUtils() {
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to given minutes.
	 *
	 * @param d       the {@link ZonedDateTime}
	 * @param minutes the minutes to round down to; max 59
	 * @return the rounded result
	 */
	public static ZonedDateTime roundDownToMinutes(ZonedDateTime d, int minutes) {
		return d.truncatedTo(DurationUnit.ofMinutes(minutes));
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to next quarter (15 minutes).
	 *
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	public static ZonedDateTime roundDownToQuarter(ZonedDateTime d) {
		return roundDownToMinutes(d, 15);
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
	 * Parses a string to an {@link ZonedDateTime} or returns null.
	 * 
	 * <p>
	 * See {@link ZonedDateTime#parse(CharSequence)}
	 * 
	 * @param date the string value
	 * @return a {@link ZonedDateTime} or null
	 */
	public static ZonedDateTime parseZonedDateTimeOrNull(String date) {
		return parseZonedDateTimeOrNull(date, DateTimeFormatter.ISO_ZONED_DATE_TIME);
	}

	/**
	 * Parses a string to an {@link ZonedDateTime} or returns null.
	 * 
	 * <p>
	 * See {@link ZonedDateTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param date      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link ZonedDateTime} or null
	 */
	public static ZonedDateTime parseZonedDateTimeOrNull(String date, DateTimeFormatter formatter) {
		return parseDateOrNull(ZonedDateTime::parse, date, formatter);
	}

	/**
	 * Parses a string to an {@link ZonedDateTime} or throws an error.
	 * 
	 * <p>
	 * See {@link ZonedDateTime#parse(CharSequence)}
	 * 
	 * @param date the string value
	 * @return a {@link ZonedDateTime}
	 * @throws OpenemsException on error
	 */
	public static ZonedDateTime parseZonedDateTimeOrError(String date) throws OpenemsException {
		return parseZonedDateTimeOrError(date, DateTimeFormatter.ISO_ZONED_DATE_TIME);
	}

	/**
	 * Parses a string to an {@link ZonedDateTime} or throws an error.
	 * 
	 * <p>
	 * See {@link ZonedDateTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param date      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link ZonedDateTime}
	 * @throws OpenemsException on error
	 */
	public static ZonedDateTime parseZonedDateTimeOrError(String date, DateTimeFormatter formatter)
			throws OpenemsException {
		return parseDateOrError(ZonedDateTime.class, ZonedDateTime::parse, date, formatter);
	}

	/**
	 * Parses a string to an {@link LocalDate} or returns null.
	 * 
	 * <p>
	 * See {@link LocalDate#parse(CharSequence)}
	 * 
	 * @param date the string value
	 * @return a {@link LocalDate} or null
	 */
	public static LocalDate parseLocalDateOrNull(String date) {
		return parseLocalDateOrNull(date, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * Parses a string to an {@link LocalDate} or returns null.
	 * 
	 * <p>
	 * See {@link LocalDate#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param date      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalDate} or null
	 */
	public static LocalDate parseLocalDateOrNull(String date, DateTimeFormatter formatter) {
		return parseDateOrNull(LocalDate::parse, date, formatter);
	}

	/**
	 * Parses a string to an {@link LocalDate} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalDate#parse(CharSequence)}
	 * 
	 * @param date the string value
	 * @return a {@link LocalDate}
	 * @throws OpenemsException on error
	 */
	public static LocalDate parseLocalDateOrError(String date) throws OpenemsException {
		return parseLocalDateOrError(date, DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * Parses a string to an {@link LocalDate} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalDate#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param date      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalDate}
	 * @throws OpenemsException on error
	 */
	public static LocalDate parseLocalDateOrError(String date, DateTimeFormatter formatter) throws OpenemsException {
		return parseDateOrError(LocalDate.class, LocalDate::parse, date, formatter);
	}

	/**
	 * Parses a string to an {@link LocalDateTime} or returns null.
	 * 
	 * <p>
	 * See {@link LocalDateTime#parse(CharSequence)}
	 * 
	 * @param dateTime date the string value
	 * @return a {@link LocalDateTime} or null
	 */
	public static LocalDateTime parseLocalDateTimeOrNull(String dateTime) {
		return parseDateOrNull(LocalDateTime::parse, dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	/**
	 * Parses a string to an {@link LocalDateTime} or returns null.
	 * 
	 * <p>
	 * See {@link LocalDateTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param dateTime  date the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalDateTime} or null
	 */
	public static LocalDateTime parseLocalDateTimeOrNull(String dateTime, DateTimeFormatter formatter) {
		return parseDateOrNull(LocalDateTime::parse, dateTime, formatter);
	}

	/**
	 * Parses a string to an {@link LocalDateTime} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalDateTime#parse(CharSequence)}
	 * 
	 * @param dateTime date the string value
	 * @return a {@link LocalDateTime}
	 * @throws OpenemsException on error
	 */
	public static LocalDateTime parseLocalDateTimeOrError(String dateTime) throws OpenemsException {
		return parseDateOrError(LocalDateTime.class, LocalDateTime::parse, dateTime,
				DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	/**
	 * Parses a string to an {@link LocalDateTime} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalDateTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param dateTime  date the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalDateTime}
	 * @throws OpenemsException on error
	 */
	public static LocalDateTime parseLocalDateTimeOrError(String dateTime, DateTimeFormatter formatter)
			throws OpenemsException {
		return parseDateOrError(LocalDateTime.class, LocalDateTime::parse, dateTime, formatter);
	}

	/**
	 * Parses a string to an {@link LocalTime} or returns null.
	 * 
	 * <p>
	 * See {@link LocalTime#parse(CharSequence)}
	 * 
	 * @param time the string value
	 * @return a {@link LocalTime} or null
	 */
	public static LocalTime parseLocalTimeOrNull(String time) {
		return parseLocalTimeOrNull(time, DateTimeFormatter.ISO_LOCAL_TIME);
	}

	/**
	 * Parses a string to an {@link LocalTime} or returns null.
	 * 
	 * <p>
	 * See {@link LocalTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param time      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalTime} or null
	 */
	public static LocalTime parseLocalTimeOrNull(String time, DateTimeFormatter formatter) {
		return parseDateOrNull(LocalTime::parse, time, formatter);
	}

	/**
	 * Parses a string to an {@link LocalTime} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalTime#parse(CharSequence)}
	 * 
	 * @param time the string value
	 * @return a {@link LocalTime}
	 * @throws OpenemsException on error
	 */
	public static LocalTime parseLocalTimeOrError(String time) throws OpenemsException {
		return parseLocalTimeOrError(time, DateTimeFormatter.ISO_LOCAL_TIME);
	}

	/**
	 * Parses a string to an {@link LocalTime} or throws an error.
	 * 
	 * <p>
	 * See {@link LocalTime#parse(CharSequence, DateTimeFormatter)}
	 * 
	 * @param time      the string value
	 * @param formatter the formatter to use, not null
	 * @return a {@link LocalTime}
	 * @throws OpenemsException on error
	 */
	public static LocalTime parseLocalTimeOrError(String time, DateTimeFormatter formatter) throws OpenemsException {
		return parseDateOrError(LocalTime.class, LocalTime::parse, time, formatter);
	}

	private static final <T> T parseDateOrNull(//
			BiFunction<String, DateTimeFormatter, T> parser, //
			String value, //
			DateTimeFormatter formatter //
	) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return parser.apply(value, formatter);
		} catch (DateTimeParseException e) {
			// unable to parse date
		} catch (RuntimeException e) {
			// unexpected error
			e.printStackTrace();
		}
		return null;
	}

	private static final <T> T parseDateOrError(//
			Class<?> clazz, //
			BiFunction<String, DateTimeFormatter, T> parser, //
			String value, //
			DateTimeFormatter formatter //
	) throws OpenemsException {
		if (value == null) {
			throw new OpenemsException(clazz.getSimpleName() + " is null");
		}
		if (value.isBlank()) {
			throw new OpenemsException(clazz.getSimpleName() + " is blank");
		}
		try {
			return parser.apply(value, formatter);
		} catch (DateTimeParseException e) {
			// unable to parse date
			throw new OpenemsException(
					"Unable to parse " + clazz.getSimpleName() + " [" + value + "] " + e.getMessage());
		} catch (RuntimeException e) {
			// unexpected error
			throw new OpenemsException("Unexpected error while trying to parse " + clazz.getSimpleName() //
					+ " [" + value + "] " + e.getMessage());
		}
	}

}
