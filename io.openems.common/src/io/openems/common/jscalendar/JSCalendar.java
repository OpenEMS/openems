package io.openems.common.jscalendar;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsOptionalUUID;
import static io.openems.common.utils.JsonUtils.getAsOptionalZonedDateTime;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.common.utils.JsonUtils.stream;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.LocalDate.EPOCH;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.util.Arrays.stream;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.utils.JsonUtils;

/**
 * Implementation of RFC 8984 "JSCalendar: A JSON Representation of Calendar
 * Data".
 * 
 * <p>
 * See <a href=
 * "https://www.rfc-editor.org/rfc/rfc8984.html">https://www.rfc-editor.org/rfc/rfc8984.html</a>
 */
// CHECKSTYLE:OFF
public class JSCalendar<PAYLOAD> {
	// CHECKSTYLE:ON

	private static final String PROPERTY_PAYLOAD = "openems.io:payload";

	/**
	 * Helper utilities to handle lists of {@link Task}s.
	 */
	public static class Tasks {
		private Tasks() {
		}

		/**
		 * Parse a List of {@link Task}s from a String representing a {@link JsonArray}
		 * - includes checks for null and empty.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param string        the {@link JsonArray} string
		 * @param payloadParser a parser for a Payload
		 * @return the List of {@link Task}s
		 */
		public static <PAYLOAD> ImmutableList<Task<PAYLOAD>> fromStringOrEmpty(String string,
				ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException> payloadParser) {
			if (string == null || string.isBlank()) {
				return ImmutableList.of();
			}
			try {
				return fromJson(parseToJsonArray(string), payloadParser);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
				return ImmutableList.of();
			}
		}

		/**
		 * Parse a List of {@link Task}s from a {@link JsonArray}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonArray}
		 * @param payloadParser a parser for a Payload
		 * @return the List of {@link Task}s
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> ImmutableList<Task<PAYLOAD>> fromJson(JsonArray json,
				ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException> payloadParser)
				throws OpenemsNamedException {
			return stream(json) //
					.map(j -> {
						try {
							return Task.fromJson(getAsJsonObject(j), payloadParser);
						} catch (OpenemsNamedException e) {
							e.printStackTrace();
							throw new NoSuchElementException(e.getMessage());
						}
					}) //
					.collect(toImmutableList());
		}

		/**
		 * Convert to a {@link JsonArray}.
		 * 
		 * @param <PAYLOAD>        the type of the Payload
		 * @param tasks            a List of {@link Task}s
		 * @param payloadConverter a converter for a Payload
		 * @return a {@link JsonArray}
		 */
		public static <PAYLOAD> JsonArray toJsonArray(ImmutableList<Task<PAYLOAD>> tasks,
				Function<PAYLOAD, JsonObject> payloadConverter) {
			return tasks.stream() //
					.map(t -> t.toJson(payloadConverter)) //
					.collect(JsonUtils.toJsonArray());
		}

		public static record OneTask<PAYLOAD>(ZonedDateTime start, Duration duration, PAYLOAD payload) {
		}

		/**
		 * Gets the next occurence of the {@link Task} (including duration) at or after
		 * a date.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @param tasks     a List of {@link Task}s
		 * @param from      the from timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public static <PAYLOAD> Optional<OneTask<PAYLOAD>> getNextOccurence(ImmutableList<Task<PAYLOAD>> tasks,
				ZonedDateTime from) {
			return tasks.stream() //
					.map(task -> {
						var start = task.getNextOccurence(from);
						return start == null //
								? null //
								: new OneTask<PAYLOAD>(start, task.duration, task.payload);
					}) //
					.filter(Objects::nonNull) //
					.sorted((ot0, ot1) -> ot0.start().compareTo(ot1.start())) //
					.findFirst();
		}

		/**
		 * Gets the occurences of the {@link Task}s (including currently active task)
		 * between two dates.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @param tasks     a List of {@link Task}s
		 * @param from      the from timestamp
		 * @param to        the to timestamp
		 * @return a list of {@link OneTask}s
		 */
		public static <PAYLOAD> ImmutableList<OneTask<PAYLOAD>> getOccurencesBetween(ImmutableList<Task<PAYLOAD>> tasks,
				ZonedDateTime from, ZonedDateTime to) {
			return tasks.stream() //
					.flatMap(t -> t.getOccurencesBetween(from, to).stream() //
							.map(s -> new OneTask<PAYLOAD>(s, t.duration, t.payload))) //
					.sorted((ot0, ot1) -> ot0.start().compareTo(ot1.start())) //
					.collect(toImmutableList());
		}
	}

	public static record Task<PAYLOAD>(UUID uid, ZonedDateTime updated, LocalDateTime start, Duration duration,
			ImmutableList<RecurrenceRule> recurrenceRules, PAYLOAD payload) {

		/**
		 * Parse a {@link Task} from a {@link JsonObject}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonObject}
		 * @param payloadParser a parser for a Payload
		 * @return the {@link Task}
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> Task<PAYLOAD> fromJson(JsonObject json, Function<JsonObject, PAYLOAD> payloadParser)
				throws OpenemsNamedException {
			return fromJson(json, new ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException>() {

				@Override
				public PAYLOAD apply(JsonObject json) throws OpenemsNamedException {
					return payloadParser.apply(json);
				}
			});
		}

		/**
		 * Parse a {@link Task} from a {@link JsonObject}.
		 * 
		 * @param <PAYLOAD>     the type of the Payload
		 * @param json          the {@link JsonObject}
		 * @param payloadParser a parser for a Payload
		 * @return the {@link Task}
		 * @throws OpenemsNamedException on error
		 */
		public static <PAYLOAD> Task<PAYLOAD> fromJson(JsonObject json,
				ThrowingFunction<JsonObject, PAYLOAD, OpenemsNamedException> payloadParser)
				throws OpenemsNamedException {
			var type = getAsString(json, "@type");
			if (!type.equalsIgnoreCase("Task")) {
				throw new OpenemsException("This is not a 'Task': " + type);
			}
			var b = Task.<PAYLOAD>create() //
					.setUid(getAsOptionalUUID(json, "uid").orElse(null)) //
					.setUpdated(getAsOptionalZonedDateTime(json, "updated").orElse(null)) //
					.setStart(getAsString(json, "start")) //
					.setDuration(getAsOptionalString(json, "duration").orElse(null)); //
			getAsOptionalJsonArray(json, "recurrenceRules") //
					.ifPresent(j -> stream(j) //
							.forEach(r -> b.addRecurrenceRule(r)));
			var rawPayload = getAsOptionalJsonObject(json, PROPERTY_PAYLOAD);
			b.setPayload(rawPayload.isPresent() //
					? payloadParser.apply(rawPayload.get()) //
					: null);
			return b.build();
		}

		public static class Builder<PAYLOAD> {
			private UUID uid = null;
			private ZonedDateTime updated = null;
			private LocalDateTime start = null;
			private Duration duration = null;
			private ImmutableList.Builder<RecurrenceRule> recurrenceRules = ImmutableList.builder();
			private PAYLOAD payload = null;

			protected Builder() {
			}

			public Builder<PAYLOAD> setUid(UUID uid) {
				this.uid = uid;
				return this;
			}

			public Builder<PAYLOAD> setUpdated(ZonedDateTime updated) {
				this.updated = updated;
				return this;
			}

			public Builder<PAYLOAD> setStart(LocalDateTime start) {
				this.start = start;
				return this;
			}

			public Builder<PAYLOAD> setStart(LocalTime start) {
				return this.setStart(LocalDateTime.of(EPOCH, start));
			}

			protected Builder<PAYLOAD> setStart(String start) throws DateTimeParseException {
				try {
					return this.setStart(LocalDateTime.parse(start));
				} catch (DateTimeParseException e) {
					return this.setStart(LocalTime.parse(start));
				}
			}

			public Builder<PAYLOAD> setDuration(Duration duration) {
				this.duration = duration;
				return this;
			}

			protected Builder<PAYLOAD> setDuration(String duration) {
				return this.setDuration(duration == null ? null : Duration.parse(duration));
			}

			/**
			 * Adds a {@link RecurrenceRule}.
			 * 
			 * @param recurrenceRule the {@link RecurrenceRule}
			 * @return myself
			 */
			public Builder<PAYLOAD> addRecurrenceRule(RecurrenceRule recurrenceRule) {
				this.recurrenceRules.add(recurrenceRule);
				return this;
			}

			/**
			 * Adds a {@link RecurrenceRule}.
			 * 
			 * @param json the {@link RecurrenceRule} as {@link JsonObject}
			 * @return myself
			 */
			public Builder<PAYLOAD> addRecurrenceRule(JsonElement json) throws NoSuchElementException {
				try {
					return this.addRecurrenceRule(RecurrenceRule.fromJson(json));
				} catch (OpenemsNamedException e) {
					e.printStackTrace();
					throw new NoSuchElementException(e.getMessage());
				}
			}

			/**
			 * Adds a {@link RecurrenceRule}.
			 * 
			 * @param consumer a RecurrenceRule Builder
			 * @return myself
			 */
			public Builder<PAYLOAD> addRecurrenceRule(Consumer<RecurrenceRule.Builder> consumer) {
				var builder = RecurrenceRule.create();
				consumer.accept(builder);
				this.recurrenceRules.add(builder.build());
				return this;
			}

			public Builder<PAYLOAD> setPayload(PAYLOAD payload) {
				this.payload = payload;
				return this;
			}

			public Task<PAYLOAD> build() {
				return new Task<PAYLOAD>(this.uid, this.updated, this.start, this.duration,
						this.recurrenceRules.build(), this.payload);
			}
		}

		/**
		 * Create a {@link CalendarEvent} {@link Builder}.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @return a {@link Builder}
		 */
		public static <PAYLOAD> Builder<PAYLOAD> create() {
			return new Builder<PAYLOAD>();
		}

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @param payloadConverter a converter for a Payload
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJson(Function<PAYLOAD, JsonObject> payloadConverter) {
			var j = JsonUtils.buildJsonObject() //
					.addProperty("@type", "Task");
			if (this.uid != null) {
				j.addProperty("uid", this.uid.toString());
			}
			if (this.updated != null) {
				j.addProperty("updated", this.updated.format(ISO_INSTANT));
			}
			if (this.start != null) {
				if (LocalDate.from(this.start).equals(EPOCH)) {
					j.addProperty("start", this.start.format(DateTimeFormatter.ISO_LOCAL_TIME));
				} else {
					j.addProperty("start", this.start.format(ISO_LOCAL_DATE_TIME));
				}
			}
			if (this.duration != null) {
				j.addProperty("duration", this.duration.toString());
			}
			if (!this.recurrenceRules.isEmpty()) {
				j.add("recurrenceRules", this.recurrenceRules.stream() //
						.map(RecurrenceRule::toJson) //
						.collect(JsonUtils.toJsonArray()));
			}
			if (this.payload != null) {
				j.add(PROPERTY_PAYLOAD, payloadConverter.apply(this.payload));
			}
			return j.build();
		}

		/**
		 * Gets the next occurence of the {@link Task} (including currently active task)
		 * at or after a date.
		 * 
		 * @param from the from timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public ZonedDateTime getNextOccurence(ZonedDateTime from) {
			final var f = this.duration == null //
					? from //
					: from.minus(this.duration); // query active tasks
			return this.recurrenceRules.stream() //
					.map(rr -> rr.getNextOccurence(this.start, f)) //
					.min((o1, o2) -> o1.toInstant().compareTo(o2.toInstant())) //
					.orElse(null);
		}

		/**
		 * Gets the occurences of the {@link Task} (including currently active task)
		 * between two dates.
		 * 
		 * @param from the from timestamp
		 * @param to   the to timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public ImmutableList<ZonedDateTime> getOccurencesBetween(ZonedDateTime from, ZonedDateTime to) {
			var result = ImmutableList.<ZonedDateTime>builder();
			for (var rr : this.recurrenceRules) {
				var nextFrom = this.duration == null //
						? from //
						: from.minus(this.duration); // query active tasks;
				while (true) {
					var start = rr.getNextOccurence(this.start, nextFrom);
					if (start.isAfter(to)) {
						break;
					}
					result.add(start);
					nextFrom = this.duration == null //
							? start.plusNanos(1) //
							: start.plus(this.duration).plusNanos(1);
				}
			}
			return result.build();
		}
	}

	public enum RecurrenceFrequency {
		// SECONDLY("secondly"),
		// MINUTELY("minutely"),
		// HOURLY("hourly"),
		DAILY("daily"), //
		WEEKLY("weekly"), //
		MONTHLY("monthly"), //
		YEARLY("yearly");

		public final String name;

		private RecurrenceFrequency(String name) {
			this.name = name;
		}
	}

	public record RecurrenceRule(RecurrenceFrequency frequency, ImmutableSortedSet<DayOfWeek> byDay) {

		/**
		 * Parse a {@link RecurrenceRule} from a {@link JsonObject}.
		 * 
		 * @param json the {@link JsonObject}
		 * @return the {@link RecurrenceRule}
		 * @throws OpenemsNamedException on error
		 */
		public static RecurrenceRule fromJson(JsonElement json) throws OpenemsNamedException, NoSuchElementException {
			var frequency = getAsEnum(RecurrenceFrequency.class, json, "frequency");
			var byDay = getAsOptionalJsonArray(json, "byDay") //
					.map(arr -> stream(arr) //
							.map(j -> switch (getAsOptionalString(j).orElseThrow()) {
							case "mo" -> MONDAY;
							case "tu" -> TUESDAY;
							case "we" -> WEDNESDAY;
							case "th" -> THURSDAY;
							case "fr" -> FRIDAY;
							case "sa" -> SATURDAY;
							case "su" -> SUNDAY;
							default -> throw new NoSuchElementException("");
							}) //
							.collect(toImmutableSortedSet(Ordering.natural()))) //
					.orElse(ImmutableSortedSet.of());
			return new RecurrenceRule(frequency, byDay);
		}

		public static class Builder {
			private RecurrenceFrequency frequency;
			private ImmutableSortedSet.Builder<DayOfWeek> byDay = ImmutableSortedSet.naturalOrder();

			public Builder() {
			}

			public Builder setFrequency(RecurrenceFrequency frequency) {
				this.frequency = frequency;
				return this;
			}

			/**
			 * Adds a `byDay`rule.
			 * 
			 * @param byDay the {@link DayOfWeek}s
			 * @return myself
			 */
			public Builder addByDay(DayOfWeek... byDay) {
				stream(byDay).forEach(this.byDay::add);
				return this;
			}

			public RecurrenceRule build() {
				return new RecurrenceRule(this.frequency, this.byDay.build());
			}
		}

		/**
		 * Create a {@link RecurrenceRule} {@link Builder}.
		 * 
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Gets the next occurence of the {@link RecurrenceRule} at or after a date.
		 * 
		 * @param taskStart the start timestamp of the {@link Task}
		 * @param from      the from date
		 * @return a {@link ZonedDateTime}
		 */
		public ZonedDateTime getNextOccurence(LocalDateTime taskStart, ZonedDateTime from) {
			final var taskStartZoned = taskStart.atZone(from.getZone());
			from = from.isBefore(taskStartZoned) //
					? taskStartZoned //
					: from;

			return switch (this.frequency) {
			case DAILY -> {
				if (from.toLocalTime().isAfter(taskStart.toLocalTime())) {
					from = from.plusDays(1);
				}
				yield from. //
						truncatedTo(DAYS) //
						.with(NANO_OF_DAY, taskStart.toLocalTime().toNanoOfDay());

			}
			case WEEKLY -> {
				if (!this.byDay.isEmpty()) {
					if (from.toLocalTime().isAfter(taskStart.toLocalTime())) {
						from = from.plusDays(1); // tomorrow
					}
					from = from.with(NANO_OF_DAY, taskStart.toLocalTime().toNanoOfDay());
					var nextByDay = this.byDay.ceiling(from.getDayOfWeek());
					if (nextByDay != null) {
						yield from.with(nextOrSame(nextByDay)); // next weekday in list
					}
					nextByDay = this.byDay.first();
					if (from.getDayOfWeek() == nextByDay) {
						yield from.plusWeeks(1); // same day next week
					}
					yield from.with(nextOrSame(this.byDay.first())); // first day in list
				}
				// TODO: If frequency is weekly and there is no byDay property, add a byDay
				// property with the sole value being the day of the week of the initial
				// date-time.
				yield null; // not implemented
			}
			case MONTHLY -> null; // not implemented
			case YEARLY -> null; // not implemented
			};
		}

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJson() {
			var j = JsonUtils.buildJsonObject();
			if (this.frequency != null) {
				j.addProperty("frequency", this.frequency.name);
			}
			if (!this.byDay.isEmpty()) {
				j.add("byDay", this.byDay.stream() //
						.map(d -> switch (d) {
						case MONDAY -> "mo";
						case TUESDAY -> "tu";
						case WEDNESDAY -> "we";
						case THURSDAY -> "th";
						case FRIDAY -> "fr";
						case SATURDAY -> "sa";
						case SUNDAY -> "su";
						}) //
						.map(JsonUtils::toJson) //
						.collect(toJsonArray()));
			}
			return j.build();
		}
	}

}
