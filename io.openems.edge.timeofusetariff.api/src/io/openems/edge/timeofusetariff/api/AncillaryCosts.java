package io.openems.edge.timeofusetariff.api;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.utils.DateUtils.parseLocalTimeOrError;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.LocalTime.MIN;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonObjectPathActual;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff;

public class AncillaryCosts {

	public record GridFee(ImmutableList<DateRange> dateRanges) {

		public record DateRange(LocalDate start, LocalDate end, ImmutableList<TimeRange> timeRanges,
				double standardTariff, double lowTariff, double highTariff) {

			public static enum Quarter {
				Q1(/* start */1, 1, /* end */3, 31), //
				Q2(/* start */4, 1, /* end */6, 30), //
				Q3(/* start */7, 1, /* end */9, 30), //
				Q4(/* start */10, 1, /* end */12, 31), //
				FULL_YEAR(/* start */1, 1, /* end */12, 31);

				protected final int startMonth;
				protected final int startDayOfMonth;
				protected final int endMonth;
				protected final int endDayOfMonth;

				private Quarter(int startMonth, int startDayOfMonth, int endMonth, int endDayOfMonth) {
					this.startMonth = startMonth;
					this.startDayOfMonth = startDayOfMonth;
					this.endMonth = endMonth;
					this.endDayOfMonth = endDayOfMonth;
				}

				public static record WithYear(int year, Quarter quarter) {
				}
			}

			protected static class Builder {
				private final ImmutableList.Builder<TimeRange> timeRanges = ImmutableList.builder();

				private ImmutableList<Quarter.WithYear> quarters = null;
				private double standardTariff = Double.NaN;
				private double lowTariff = Double.NaN;
				private double highTariff = Double.NaN;

				private Builder() {
				}

				public Builder setQuarters(int year, Quarter... quarters) {
					asserNotNull("Quarters", this.quarters);
					this.quarters = Arrays.stream(quarters) //
							.map(q -> new Quarter.WithYear(year, q)) //
							.collect(ImmutableList.toImmutableList());
					return this;
				}

				public Builder setStandardTariff(double standardTariff) {
					asserNotNaN("StandardTariff", this.standardTariff);
					this.standardTariff = standardTariff;
					return this;
				}

				public Builder setLowTariff(double lowTariff) {
					asserNotNaN("LowTariff", this.lowTariff);
					this.lowTariff = lowTariff;
					return this;
				}

				public Builder setHighTariff(double highTariff) {
					asserNotNaN("HighTariff", this.highTariff);
					this.highTariff = highTariff;
					return this;
				}

				public Builder addTimeRange(TimeRange timeRange) {
					this.timeRanges.add(timeRange);
					return this;
				}

				public Builder addTimeRange(Consumer<TimeRange.Builder> timeRangeBuilder) {
					var b = new TimeRange.Builder();
					timeRangeBuilder.accept(b);
					return this.addTimeRange(b.build());
				}

				public Stream<DateRange> build() {
					TypeUtils.assertNull("Quarters", (Object) this.quarters);
					return this.quarters.stream() //
							.map(q -> new DateRange(//
									LocalDate.of(q.year, q.quarter.startMonth, q.quarter.startDayOfMonth), //
									LocalDate.of(q.year, q.quarter.endMonth, q.quarter.endDayOfMonth), //
									this.timeRanges.build(), //
									this.standardTariff, this.lowTariff, this.highTariff));
				}

				private static void asserNotNull(String description, Object value) {
					if (value != null) {
						throw new IllegalArgumentException(description + " had already been set to [" + value + "]");
					}
				}

				private static void asserNotNaN(String description, double value) {
					if (!Double.isNaN(value)) {
						throw new IllegalArgumentException(description + " had already been set to [" + value + "]");
					}
				}
			}
		}

		public static enum Tariff {
			STANDARD, LOW, HIGH;
		}

		public record TimeRange(LocalTime start, LocalTime end, Tariff tariff) {

			protected static class Builder {
				private LocalTime start;
				private LocalTime end;
				private Tariff tariff;

				private Builder() {
				}

				public Builder setStart(LocalTime start) {
					this.start = start;
					return this;
				}

				public Builder setStart(int hour, int minute) {
					return this.setStart(LocalTime.of(hour, minute));
				}

				public Builder setEnd(LocalTime end) {
					this.end = end;
					return this;
				}

				public Builder setEnd(int hour, int minute) {
					return this.setEnd(LocalTime.of(hour, minute));
				}

				public Builder setFullDay() {
					this.setStart(MIN);
					this.setEnd(MIDNIGHT);
					return this;
				}

				public Builder setTariff(Tariff tariff) {
					this.tariff = tariff;
					return this;
				}

				public TimeRange build() {
					return new TimeRange(this.start, this.end, this.tariff);
				}
			}
		}

		public static class Builder {
			private final ImmutableList.Builder<DateRange> dateRanges = ImmutableList.builder();

			private Builder() {
			}

			/**
			 * Adds a DateRange to this builder by applying the given consumer to a
			 * DateRange.Builder.
			 * 
			 * @param dateRange a consumer that configures the DateRange.Builder
			 * @return this builder instance for method chaining
			 */
			public Builder addDateRange(Consumer<DateRange.Builder> dateRange) {
				var drs = new DateRange.Builder();
				dateRange.accept(drs);
				drs.build() //
						.forEach(this.dateRanges::add);
				return this;
			}

			public GridFee build() {
				// Sort by start to fulfill JUnit tests with no gaps
				var drs = ImmutableList.sortedCopyOf((dr1, dr2) -> dr1.start().compareTo(dr2.start()),
						this.dateRanges.build());
				return new GridFee(drs);
			}
		}

		/**
		 * Create a GridFee builder.
		 * 
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Converts a structured list of {@code DateRange} and {@code TimeRange}
		 * definitions into a {@link JSCalendar.Tasks} object, each Task representing a
		 * non-repeating time window with an associated price (tariff).
		 * 
		 * @param clock The {!link Clock}.
		 * @return a {@link JSCalendar.Tasks} instance representing the full tariff
		 *         schedule.
		 */
		public JSCalendar.Tasks<Double> toSchedule(Clock clock) {
			final var tasks = JSCalendar.Tasks.<Double>create() //
					.setClock(clock);

			// Process all DateRanges defined in the GridFee configuration
			for (var dateRange : this.dateRanges) {

				// Process all TimeRanges for this day
				for (var timeRange : dateRange.timeRanges()) {

					// Overnight Handling
					final var duration = calculateDuration(timeRange.start, timeRange.end);

					// Determine the payload based on the tariff type.
					final var payload = switch (timeRange.tariff()) {
					case LOW -> dateRange.lowTariff;
					case STANDARD -> dateRange.standardTariff;
					case HIGH -> dateRange.highTariff;
					};

					final var startDateTime = LocalDateTime.of(dateRange.start(), timeRange.start());
					tasks.add(t -> t //
							.setStart(startDateTime) //
							.setDuration(duration) //
							.addRecurrenceRule(b -> b.setFrequency(DAILY) //
									.setUntil(dateRange.end())) //
							.setPayload(payload)//
							.build());
				}
			}

			return tasks.build();
		}

		/**
		 * Determines the standard grid fee tariff applicable at the specified date and
		 * time.
		 * 
		 * <p>
		 * This method searches through the configured date ranges and their associated
		 * time ranges to find the applicable standard tariff for the given timestamp.
		 * </p>
		 *
		 * @param time the {@link ZonedDateTime} for which to determine the applicable
		 *             standard tariff
		 * @return the standard grid fee tariff in ct/kWh for the given time, or 0.0 if
		 *         no matching date/time range is found or if no date ranges are
		 *         configured
		 * 
		 * @see #isWithinDateRange(DateRange, LocalDate)
		 * @see #isWithinTimeRange(TimeRange, LocalTime)
		 * 
		 */
		public double getStandardTariffAt(ZonedDateTime time) {

			if (this.dateRanges == null || this.dateRanges.isEmpty()) {
				return 0.0;
			}

			final var localDate = time.toLocalDate();
			final var localTime = time.toLocalTime();

			for (var dateRange : this.dateRanges) {
				if (!isWithinDateRange(dateRange, localDate)) {
					continue;
				}

				for (var timeRange : dateRange.timeRanges()) {
					if (isWithinTimeRange(timeRange, localTime)) {
						return dateRange.standardTariff();
					}
				}
			}

			return 0.0;
		}

		private static boolean isWithinDateRange(DateRange dateRange, LocalDate date) {
			return (date.isEqual(dateRange.start()) || date.isAfter(dateRange.start()))
					&& (date.isEqual(dateRange.end()) || date.isBefore(dateRange.end()));
		}

		private static boolean isWithinTimeRange(TimeRange timeRange, LocalTime time) {
			var start = timeRange.start();
			var end = timeRange.end();

			if (start.equals(end)) {
				// Full day range
				return true;
			}

			if (start.isBefore(end)) {
				// Normal case (e.g. 05:00–17:00)
				return !time.isBefore(start) && time.isBefore(end);
			} else {
				// Overnight case (e.g. 21:00–00:00)
				return (!time.isBefore(start) || time.isBefore(end));
			}
		}

	}

	/**
	 * Parses a grid fee schedule specifically for Germany from the given ancillary
	 * cost JSON string.
	 * 
	 * <p>
	 * If a valid {@code dso} (Distribution System Operator) is specified and mapped
	 * to a known {@link GermanDSO} enum constant, the corresponding predefined
	 * {@link GridFee} schedule is returned.
	 * 
	 * <p>
	 * If the {@code dso} field is missing or contains an invalid value (e.g.,
	 * {@code "OTHER"}, {@code null}, or an unrecognized string), this method falls
	 * back to parsing a custom schedule from the {@code "schedule"} JSON array
	 * using {@link #parseSchedule(JsonArray)}.
	 * 
	 * @param clock          The {@link Clock}
	 * @param ancillaryCosts the JSON configuration object
	 * @return {@link JSCalendar.Tasks} representing daily recurring tariff
	 *         intervals.
	 * @throws OpenemsNamedException on error
	 */
	public static JSCalendar.Tasks<Double> parseForGermany(Clock clock, String ancillaryCosts)
			throws OpenemsNamedException {

		var j = new JsonObjectPathActual.JsonObjectPathActualNonNull(parseToJsonObject(ancillaryCosts));

		try {
			var dsoOpt = j.getOptionalEnum("dso", GermanDSO.class);
			if (dsoOpt.isPresent()) {
				return dsoOpt.get().gridFee.toSchedule(clock);
			}
		} catch (IllegalArgumentException e) {
			// Invalid enum value like "OTHER" or "null"
		}

		var schedule = j.getJsonArrayOrNull("schedule");

		if (schedule == null) {
			return JSCalendar.Tasks.empty();
		}

		return parseSchedule(clock, j.getJsonArray("schedule"));
	}

	/**
	 * Retrieves the standard ancillary price for Germany based on the provided
	 * configuration.
	 * 
	 * <p>
	 * This method processes the ancillary costs configuration to determine the
	 * standard price for a given timestamp. It first attempts to use a predefined
	 * DSO (Distribution System Operator) configuration if available, falling back
	 * to a custom schedule if no DSO is specified.
	 * </p>
	 * 
	 * @param ancillaryCosts the ancillary costs configuration as a JSON string,
	 *                       which may contain either a "dso" field with a
	 *                       {@link GermanDSO} value or a "schedule" field with
	 *                       custom pricing rules
	 * @param time           the {@link ZonedDateTime} for which to retrieve the
	 *                       standard price
	 * @return the standard ancillary price in ct/kWh for the given time, or 0.0 if
	 *         no matching configuration is found or the schedule is null
	 * @throws OpenemsNamedException if the configuration JSON is malformed or
	 *                               cannot be parsed
	 * 
	 * @see GermanDSO
	 * @see #parseScheduleForStandardPrice(JsonArray, ZonedDateTime)
	 * 
	 */
	public static double getScheduleStandardPriceForGermany(String ancillaryCosts, ZonedDateTime time)
			throws OpenemsNamedException {
		var j = new JsonObjectPathActual.JsonObjectPathActualNonNull(parseToJsonObject(ancillaryCosts));

		try {
			var dsoOpt = j.getOptionalEnum("dso", GermanDSO.class);
			if (dsoOpt.isPresent()) {
				return dsoOpt.get().gridFee.getStandardTariffAt(time);
			}
		} catch (IllegalArgumentException e) {
			// Invalid enum value like "OTHER" or "null"
		}

		var schedule = j.getJsonArrayOrNull("schedule");

		if (schedule == null) {
			return 0.0;
		}

		return parseScheduleForStandardPrice(j.getJsonArray("schedule"), time);
	}

	private static double parseScheduleForStandardPrice(JsonArray schedule, ZonedDateTime time)
			throws OpenemsNamedException {

		for (var yearData : schedule) {
			var year = getAsInt(yearData, "year");
			var tariffs = getAsJsonObject(yearData, "tariffs");

			if (time.getYear() == year) {
				return getAsDouble(tariffs, "standard");
			}
		}

		return 0.0;
	}

	/**
	 * Parses a JSON-based tariff schedule into a {@link JSCalendar.Tasks} instance,
	 * each Task representing a time-bound tariff applied daily over a specified
	 * quarter.
	 * 
	 * <p>
	 * The input JSON is expected to be structured by year and quarters, with daily
	 * time intervals for each tariff type (`lowTariff`, `standardTariff`,
	 * `highTariff`).
	 * 
	 * @param clock    The {@link Clock}
	 * @param schedule A JSON array containing yearly tariff schedules structured by
	 *                 quarter.
	 * @return A {@link JSCalendar.Tasks} object representing daily recurring tariff
	 *         intervals.
	 * @throws OpenemsNamedException on error.
	 */
	public static JSCalendar.Tasks<Double> parseSchedule(Clock clock, JsonArray schedule) throws OpenemsNamedException {
		final var tasks = JSCalendar.Tasks.<Double>create() //
				.setClock(clock);

		for (var yearData : schedule) {
			var year = getAsInt(yearData, "year");
			var tariffs = getAsJsonObject(yearData, "tariffs");
			var quarters = getAsJsonArray(yearData, "quarters");

			var lowTariff = getAsDouble(tariffs, "low");
			var highTariff = getAsDouble(tariffs, "high");
			var standardTariff = getAsDouble(tariffs, "standard");

			// Check tariff ordering
			if (!(lowTariff < standardTariff && standardTariff < highTariff)) {
				throw new OpenemsException(
						"Tariff ordering incorrect for year " + year + ". Expected: low < standard < high");
			}

			for (var quarter : quarters) {
				var quarterNumber = getAsInt(quarter, "quarter");
				var q = Quarter.of(year, quarterNumber);

				var dailySchedules = getAsJsonArray(quarter, "dailySchedule");

				var hasFullDaySchedule = false;
				for (var dailySchedule : dailySchedules) {
					var fromStr = getAsString(dailySchedule, "from");
					var toStr = getAsString(dailySchedule, "to");
					if ("00:00".equals(fromStr) && "00:00".equals(toStr)) {
						hasFullDaySchedule = true;
						break;
					}
				}

				if (hasFullDaySchedule && dailySchedules.size() > 1) {
					throw new OpenemsException("A full-day tariff (00:00-00:00) is defined for Quarter " + quarterNumber
							+ ". No other time slots are allowed.");
				}

				var lowTariffIntervals = new ArrayList<TimeInterval>();
				var highTariffIntervals = new ArrayList<TimeInterval>();
				var standardTariffIntervals = new ArrayList<TimeInterval>();

				for (var dailySchedule : dailySchedules) {
					var tariff = JsonUtils.getAsEnum(Tariff.class, dailySchedule, "tariff");
					var fromStr = getAsString(dailySchedule, "from");
					var toStr = getAsString(dailySchedule, "to");

					final LocalTime fromTime;
					final LocalTime toTime;
					try {
						fromTime = parseLocalTimeOrError(fromStr);
						toTime = parseLocalTimeOrError(toStr);
					} catch (Exception e) {
						throw new OpenemsException("Invalid time format in " + " from=" + fromStr + ", to=" + toStr);
					}

					// from must not be after to
					if (fromTime.isAfter(toTime) && !toTime.equals(MIDNIGHT)) {
						throw new OpenemsException(
								"Invalid time range: " + " 'from' (" + fromStr + ") is after 'to' (" + toStr + ")");
					}

					var currentInterval = new TimeInterval(fromTime, toTime);

					switch (tariff) {
					case LOW -> {
						for (var range : highTariffIntervals) {
							if (currentInterval.overlapsWith(range)) {
								throw new OpenemsException("Overlap in quarter " + quarterNumber + ": lowTariff "
										+ currentInterval + " overlaps with highTariff " + range);
							}
						}
						lowTariffIntervals.add(currentInterval);
					}
					case HIGH -> {
						for (var range : lowTariffIntervals) {
							if (currentInterval.overlapsWith(range)) {
								throw new OpenemsException("Overlap in quarter " + quarterNumber + ": highTariff "
										+ currentInterval + " overlaps with lowTariff " + range);
							}
						}
						highTariffIntervals.add(currentInterval);
					}
					case STANDARD -> {
						standardTariffIntervals.add(currentInterval);
					}
					}

					// Determine the payload based on the tariff type.
					final var payload = switch (tariff) {
					case LOW -> lowTariff;
					case STANDARD -> standardTariff;
					case HIGH -> highTariff;
					};

					var duration = calculateDuration(fromTime, toTime);
					var taskStart = LocalDateTime.of(q.start, fromTime);

					tasks.add(t -> t //
							.setStart(taskStart) //
							.setDuration(duration) //
							.addRecurrenceRule(rr -> rr //
									.setFrequency(DAILY) //
									.setUntil(q.end)) //
							.setPayload(payload) //
							.build());
				}

				// Create list of all intervals for gap detection
				var allTariffIntervals = new ArrayList<>(lowTariffIntervals);
				allTariffIntervals.addAll(highTariffIntervals);
				allTariffIntervals.addAll(standardTariffIntervals);
				allTariffIntervals.sort(Comparator.comparing(a -> a.from));

				var cursor = MIN;

				for (var interval : allTariffIntervals) {
					if (cursor.isBefore(interval.from)) {
						var stdInterval = new TimeInterval(cursor, interval.from);

						var duration = calculateDuration(stdInterval.from, stdInterval.to);
						var taskStart = LocalDateTime.of(q.start, stdInterval.from);

						tasks.add(t -> t //
								.setStart(taskStart) //
								.setDuration(duration) //
								.addRecurrenceRule(rr -> rr.setFrequency(DAILY) //
										.setUntil(q.end))
								.setPayload(standardTariff) //
								.build());
					}

					// Special handling for midnight end time
					if (interval.to.equals(MIDNIGHT)) {
						cursor = MAX; // Consider it as end of day
					} else {
						cursor = cursor.isAfter(interval.to) ? cursor : interval.to;
					}
				}

				// adding final gap if we haven't reached end of day
				if (cursor.isBefore(MAX)) {
					var stdInterval = new TimeInterval(cursor, MAX);

					var duration = calculateDuration(stdInterval.from, stdInterval.to);
					var taskStart = LocalDateTime.of(q.start, stdInterval.from);

					tasks.add(t -> t //
							.setStart(taskStart) //
							.setDuration(duration) //
							.addRecurrenceRule(rr -> rr.setFrequency(DAILY) //
									.setUntil(q.end)) //
							.setPayload(standardTariff) //
							.build());
				}
			}
		}
		return tasks.build();
	}

	private static Duration calculateDuration(LocalTime start, LocalTime end) {
		if (start.isBefore(end)) {
			return Duration.between(start, end);
		} else {
			return Duration.ofHours(24).minus(Duration.between(end, start));
		}
	}

	private static class TimeInterval {
		private final LocalTime from;
		private final LocalTime to;

		public TimeInterval(LocalTime from, LocalTime to) {
			this.from = from;
			this.to = to;
		}

		/**
		 * Overlap checker. An overlap occurs only if the start of one interval is
		 * strictly before the end of the other, AND vice-versa.
		 * 
		 * @param other The other TimeInterval to check against.
		 * @return true if they overlap, false otherwise.
		 */
		public boolean overlapsWith(TimeInterval other) {
			return this.from.isBefore(other.to) && other.from.isBefore(this.to);
		}

		@Override
		public String toString() {
			return this.from + " - " + this.to;
		}
	}

	// Quarter record for date handling
	private static record Quarter(LocalDate start, LocalDate end) {
		public static Quarter of(int year, int quarter) throws OpenemsException {
			return switch (quarter) {
			case 1 -> new Quarter(LocalDate.of(year, 1, 1), LocalDate.of(year, 3, 31));
			case 2 -> new Quarter(LocalDate.of(year, 4, 1), LocalDate.of(year, 6, 30));
			case 3 -> new Quarter(LocalDate.of(year, 7, 1), LocalDate.of(year, 9, 30));
			case 4 -> new Quarter(LocalDate.of(year, 10, 1), LocalDate.of(year, 12, 31));
			default -> throw new OpenemsException("Invalid quarter: " + quarter);
			};
		}
	}
}
