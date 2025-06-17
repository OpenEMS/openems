package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;

import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.jsonrpc.serialization.JsonObjectPath;
import io.openems.common.jsonrpc.serialization.JsonObjectPathActual;

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
					this.setStart(LocalTime.MIN);
					this.setEnd(LocalTime.MAX);
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

			ImmutableList.Builder<Task<Double>> tasks = ImmutableList.builder();

			// Process all DateRanges defined in the GridFee configuration
			for (var dateRange : this.dateRanges) {

				// Process all TimeRanges for this day
				for (var timeRange : dateRange.timeRanges()) {

					// Overnight Handling
					final Duration duration;
					if (timeRange.start.isBefore(timeRange.end)) {
						duration = Duration.between(timeRange.start, timeRange.end);
					} else {
						duration = Duration.ofHours(24).minus(Duration.between(timeRange.end, timeRange.start));
					}

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
	 * Parses the ancillary cost configuration JSON into a schedule of
	 * {@link Task}s.
	 * 
	 * @param biddingZone    the {@link BiddingZone}
	 * @param ancillaryCosts the JSON configuration object
	 * @param logWarn        a {@link Consumer} for a warning message
	 * @return an {@link ImmutableList} of {@link Task} instances representing the
	 *         schedule
	 */
	public static ImmutableList<Task<Double>> parseToSchedule(BiddingZone biddingZone, String ancillaryCosts,
			Consumer<String> logWarn) {
		if (ancillaryCosts == null || ancillaryCosts.isBlank()) {
			return ImmutableList.of();
		}
		try {
			var j = new JsonObjectPathActual.JsonObjectPathActualNonNull(parseToJsonObject(ancillaryCosts));

			return switch (biddingZone) {
			case GERMANY //
				-> parseForGermany(j);
			case AUSTRIA, BELGIUM, NETHERLANDS, SWEDEN_SE1, SWEDEN_SE2, SWEDEN_SE3, SWEDEN_SE4 -> {
				logWarn.accept("Parser for " + biddingZone.name() + "-Scheduler is not implemented");
				yield ImmutableList.of();
			}
			};

		} catch (Exception e) {
			logWarn.accept("Unable to parse Schedule: " + e.getMessage());
			e.printStackTrace();
			return ImmutableList.of();
		}
	}

	private static ImmutableList<Task<Double>> parseForGermany(JsonObjectPath j) {
		return j.getOptionalEnum("dso", GermanDSO.class) //
				.map(dso -> dso.gridFee.toSchedule()) //
				.orElseGet(() -> parseSchedule(j.getJsonArray("schedule")));
	}

	private static ImmutableList<Task<Double>> parseSchedule(JsonArray schedule) {
		return ImmutableList.of(); // TODO
	}
}
