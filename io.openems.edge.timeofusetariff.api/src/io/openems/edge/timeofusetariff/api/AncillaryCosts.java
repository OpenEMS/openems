package io.openems.edge.timeofusetariff.api;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.utils.DateUtils.parseLocalTimeOrError;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.LocalTime.MIN;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.JsonObjectPath;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff;

public class AncillaryCosts {

	public record GridFee(ImmutableList<DateRange> dateRanges) {

		public record DateRange(LocalDate start, LocalDate end, ImmutableList<TimeRange> timeRanges,
				double standardTariff, double lowTariff, double highTariff) {

			protected static class Builder {
				private final ImmutableList.Builder<TimeRange> timeRanges = ImmutableList.builder();

				private LocalDate start;
				private LocalDate end;
				private double standardTariff;
				private double lowTariff;
				private double highTariff;

				private Builder() {
				}

				public Builder setStart(LocalDate start) {
					this.start = start;
					return this;
				}

				public Builder setStart(int year, int month, int dayOfMonth) {
					return this.setStart(LocalDate.of(year, month, dayOfMonth));
				}

				public Builder setEnd(LocalDate end) {
					this.end = end;
					return this;
				}

				public Builder setEnd(int year, int month, int dayOfMonth) {
					return this.setEnd(LocalDate.of(year, month, dayOfMonth));
				}

				public Builder setStandardTariff(double standardTariff) {
					this.standardTariff = standardTariff;
					return this;
				}

				public Builder setLowTariff(double lowTariff) {
					this.lowTariff = lowTariff;
					return this;
				}

				public Builder setHighTariff(double highTariff) {
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

				public DateRange build() {
					return new DateRange(this.start, this.end, this.timeRanges.build(), this.standardTariff,
							this.lowTariff, this.highTariff);
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
				var dr = new DateRange.Builder();
				dateRange.accept(dr);
				this.dateRanges.add(dr.build());
				return this;
			}

			public GridFee build() {
				return new GridFee(this.dateRanges.build());
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
		 * definitions into a flat {@link ImmutableList} of {@link Task} objects, each
		 * representing a non-repeating time window with an associated price (tariff).
		 * 
		 * @return an {@link ImmutableList} of {@link Task} instances representing the
		 *         full tariff schedule.
		 */
		public ImmutableList<Task<Double>> toSchedule() {
			final var tasks = ImmutableList.<Task<Double>>builder();

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
					final var task = Task.<Double>create() //
							.setStart(startDateTime) //
							.setDuration(duration) //
							.addRecurrenceRule(b -> b.setFrequency(DAILY) //
									.setUntil(dateRange.end())) //
							.setPayload(payload)//
							.build();

					tasks.add(task);
				}
			}

			return tasks.build();
		}
	}

	/**
	 * Parses a grid fee schedule specifically for Germany based on the given JSON
	 * input.
	 * 
	 * <p>
	 * If a valid {@code dso} (Distribution System Operator) is specified and mapped
	 * to a known {@link GermanDSO} enum constant, the corresponding predefined
	 * {@link GridFee} schedule is returned.
	 * 
	 * <p>
	 * If the {@code dso} is not present or is invalid (e.g., {@code "OTHER"},
	 * {@code null}, or unrecognized), this method falls back to parsing a custom
	 * schedule from the {@code "schedule"} JSON array using
	 * {@link #parseSchedule(JsonArray)}.
	 * 
	 * @param j A {@link JsonObjectPath} object containing the grid fee data
	 *          including optional {@code dso} and {@code schedule}.
	 * @return A list of {@link Task} instances representing daily recurring tariff
	 *         intervals.
	 * @throws OpenemsNamedException on error
	 */
	public static ImmutableList<Task<Double>> parseForGermany(JsonObjectPath j) throws OpenemsNamedException {

		try {
			var dsoOpt = j.getOptionalEnum("dso", GermanDSO.class);
			if (dsoOpt.isPresent()) {
				return dsoOpt.get().gridFee.toSchedule();
			}
		} catch (IllegalArgumentException e) {
			// Invalid enum value like "OTHER" or "null"
		}

		var schedule = j.getJsonArrayOrNull("schedule");

		if (schedule == null) {
			return ImmutableList.of();
		}

		return parseSchedule(j.getJsonArray("schedule"));

	}

	/**
	 * Parses a JSON-based tariff schedule into a list of recurring {@link Task}
	 * instances, each representing a time-bound tariff applied daily over a
	 * specified quarter.
	 * 
	 * <p>
	 * The input JSON is expected to be structured by year and quarters, with daily
	 * time intervals for each tariff type (`lowTariff`, `standardTariff`,
	 * `highTariff`).
	 * 
	 * @param schedule A JSON array containing yearly tariff schedules structured by
	 *                 quarter.
	 * @return A list of {@link Task} objects representing daily recurring tariff
	 *         intervals.
	 * @throws OpenemsNamedException OpenemsNamedException on error.
	 */
	public static ImmutableList<Task<Double>> parseSchedule(JsonArray schedule) throws OpenemsNamedException {
		final var tasks = ImmutableList.<Task<Double>>builder();

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

					var task = Task.<Double>create() //
							.setStart(taskStart) //
							.setDuration(duration) //
							.addRecurrenceRule(rr -> rr //
									.setFrequency(DAILY) //
									.setUntil(q.end)) //
							.setPayload(payload) //
							.build();

					tasks.add(task);
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

						var stdTask = Task.<Double>create() //
								.setStart(taskStart) //
								.setDuration(duration) //
								.addRecurrenceRule(rr -> rr.setFrequency(DAILY) //
										.setUntil(q.end))
								.setPayload(standardTariff) //
								.build();

						tasks.add(stdTask);
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

					var stdTask = Task.<Double>create() //
							.setStart(taskStart) //
							.setDuration(duration) //
							.addRecurrenceRule(rr -> rr.setFrequency(DAILY) //
									.setUntil(q.end)) //
							.setPayload(standardTariff) //
							.build();

					tasks.add(stdTask);
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

		public boolean overlapsWith(TimeInterval other) {
			return !(this.to.isBefore(other.from) || this.from.isAfter(other.to));
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
