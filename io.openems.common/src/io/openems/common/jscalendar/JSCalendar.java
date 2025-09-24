package io.openems.common.jscalendar;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
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
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.jsonrpc.serialization.StringParser;

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

	public static final JsonSerializer<Void> VOID_SERIALIZER = jsonObjectSerializer(//
			Void.class, //
			json -> null, //
			obj -> null);

	/**
	 * Helper utilities to handle lists of {@link Task}s.
	 */
	public static class Tasks {
		private Tasks() {
		}

		/**
		 * Returns a {@link JsonSerializer} for {@link Task}s.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<ImmutableList<Task<PAYLOAD>>> serializer(
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return Task.serializer(payloadSerializer).toImmutableListSerializer();
		}

		/**
		 * Parse a List of {@link Task}s from a String representing a {@link JsonArray}
		 * - includes checks for null and empty.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param string            the {@link JsonArray} string
		 * @param payloadSerializer a {@link JsonSerializer} for a Payload
		 * @return the List of {@link Task}s
		 */
		public static <PAYLOAD> ImmutableList<Task<PAYLOAD>> fromStringOrEmpty(String string,
				JsonSerializer<PAYLOAD> payloadSerializer) {
			if (string == null || string.isBlank()) {
				return ImmutableList.of();
			}
			try {
				return Tasks.serializer(payloadSerializer) //
						.deserialize(string);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
				return ImmutableList.of();
			}
		}

		/**
		 * Parse a List of {@link Task}s without Payload from a String representing a
		 * {@link JsonArray} - includes checks for null and empty.
		 * 
		 * @param string the {@link JsonArray} string
		 * @return the List of {@link Task}s
		 */
		public static ImmutableList<Task<Void>> fromStringOrEmpty(String string) {
			return fromStringOrEmpty(string, VOID_SERIALIZER);
		}

		/**
		 * Holds data of one task.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 */
		public static record OneTask<PAYLOAD>(ZonedDateTime start, Duration duration, ZonedDateTime end,
				PAYLOAD payload) {

			/**
			 * Builds a {@link OneTask}.
			 * 
			 * @param <PAYLOAD> the type of the Payload
			 * @param start     the start timestamp
			 * @param duration  the {@link Duration}
			 * @param payload   the Payload
			 * @return the {@link OneTask}
			 */
			public static <PAYLOAD> OneTask<PAYLOAD> from(ZonedDateTime start, Duration duration, PAYLOAD payload) {
				return new OneTask<PAYLOAD>(start, duration, start.plus(duration), payload);
			}
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
								: OneTask.<PAYLOAD>from(start, task.duration, task.payload);
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
							.map(s -> OneTask.<PAYLOAD>from(s, t.duration, t.payload))) //
					.sorted((ot0, ot1) -> ot0.start().compareTo(ot1.start())) //
					.collect(toImmutableList());
		}
	}

	public static record Task<PAYLOAD>(UUID uid, ZonedDateTime updated, LocalDateTime start, Duration duration,
			ImmutableList<RecurrenceRule> recurrenceRules, PAYLOAD payload) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Task} without payload.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Task<Void>> serializer() {
			return serializer(VOID_SERIALIZER);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Task}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Task<PAYLOAD>> serializer(JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Task<PAYLOAD>>jsonObjectSerializer(json -> {
				var type = json.getString("@type");
				if (!type.equalsIgnoreCase("Task")) {
					throw new IllegalArgumentException("This is not a 'Task': " + type);
				}

				var b = Task.<PAYLOAD>create() //
						.setUid(json.getUuidOrNull("uid")) //
						.setUpdated(json.getZonedDateTimeOrNull("updated")) //
						.setStart(json.getString("start")) //
						.setDuration(json.getStringOrNull("duration")); //

				json.getOptionalList("recurrenceRules", RecurrenceRule.serializer()) //
						.orElse(emptyList()) //
						.forEach(rr -> b.addRecurrenceRule(rr));

				var payload = json.getObjectOrNull(PROPERTY_PAYLOAD, payloadSerializer);
				if (payload != null) {
					b.setPayload(payload);
				}

				return b.build();
			}, obj -> {
				return buildJsonObject() //
						.addProperty("@type", "Task") //
						.onlyIf(obj.uid != null, //
								j -> j.addProperty("uid", obj.uid.toString())) //
						.onlyIf(obj.updated != null, //
								j -> j.addProperty("updated", obj.updated.format(ISO_INSTANT))) //
						.onlyIf(obj.start != null, //
								j -> j.addProperty("start", LocalDate.from(obj.start).equals(EPOCH) //
										? obj.start.format(DateTimeFormatter.ISO_LOCAL_TIME) //
										: obj.start.format(ISO_LOCAL_DATE_TIME))) //
						.onlyIf(obj.duration != null, //
								j -> j.addProperty("duration", obj.duration.toString())) //
						.onlyIf(!obj.recurrenceRules.isEmpty(), //
								j -> j.add("recurrenceRules", obj.recurrenceRules.stream() //
										.map(RecurrenceRule.serializer()::serialize) //
										.collect(toJsonArray()))) //
						.onlyIf(obj.payload != null, //
								j -> j.add(PROPERTY_PAYLOAD, payloadSerializer.serialize(obj.payload))) //
						.build();
			});
		}

		public static class Builder<PAYLOAD> {
			private final ImmutableList.Builder<RecurrenceRule> recurrenceRules = ImmutableList.builder();

			private UUID uid = null;
			private ZonedDateTime updated = null;
			private LocalDateTime start = null;
			private Duration duration = null;
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
					.filter(Objects::nonNull) //
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

	public record RecurrenceRule(RecurrenceFrequency frequency, LocalDate until, ImmutableSortedSet<DayOfWeek> byDay) {
		// TODO "until" is defined as LocalDateTime in the RFC
		// https://www.rfc-editor.org/rfc/rfc8984.html#section-4.3.3

		/**
		 * Returns a {@link JsonSerializer} for a {@link RecurrenceRule}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<RecurrenceRule> serializer() {
			return jsonObjectSerializer(RecurrenceRule.class, json -> {
				return new RecurrenceRule(//
						json.getEnum("frequency", RecurrenceFrequency.class), //
						json.getOptionalLocalDate("until").orElse(null), //
						json.getNullableJsonArrayPath("byDay") //
								.mapToOptional(t -> t.getAsImmutableSortedSet(//
										dayOfWeekSerializer()::deserializePath, Ordering.natural()))
								.orElse(ImmutableSortedSet.of()));
			}, obj -> {
				return buildJsonObject() //
						.addPropertyIfNotNull("frequency", obj.frequency().name().toLowerCase()) //
						.addPropertyIfNotNull("until", obj.until()) //
						.onlyIf(!obj.byDay.isEmpty(),
								j -> j.add("byDay", dayOfWeekSerializer().toSetSerializer().serialize(obj.byDay)))
						.build();
			});
		}

		private static JsonSerializer<DayOfWeek> dayOfWeekSerializer() {
			return jsonSerializer(DayOfWeek.class, json -> {
				return json.getAsStringParsed(string -> switch (string) {
				case "mo" -> MONDAY;
				case "tu" -> TUESDAY;
				case "we" -> WEDNESDAY;
				case "th" -> THURSDAY;
				case "fr" -> FRIDAY;
				case "sa" -> SATURDAY;
				case "su" -> SUNDAY;
				default -> throw new OpenemsRuntimeException("Unable to parse value '" + string + "' to DayOfWeek");
				}, () -> new StringParser.ExampleValues<>("mo", MONDAY));
			}, obj -> {
				return new JsonPrimitive(switch (obj) {
				case MONDAY -> "mo";
				case TUESDAY -> "tu";
				case WEDNESDAY -> "we";
				case THURSDAY -> "th";
				case FRIDAY -> "fr";
				case SATURDAY -> "sa";
				case SUNDAY -> "su";
				});
			});
		}

		public static class Builder {
			private RecurrenceFrequency frequency;
			private LocalDate until;
			private ImmutableSortedSet.Builder<DayOfWeek> byDay = ImmutableSortedSet.naturalOrder();

			public Builder() {
			}

			public Builder setFrequency(RecurrenceFrequency frequency) {
				this.frequency = frequency;
				return this;
			}

			public Builder setUntil(LocalDate until) {
				this.until = until;
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
				return new RecurrenceRule(this.frequency, this.until, this.byDay.build());
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
				// Adjust 'from' if the time of day has already passed
				if (from.toLocalTime().isAfter(taskStart.toLocalTime())) {
					from = from.plusDays(1); // tomorrow
				}
				from = from.with(NANO_OF_DAY, taskStart.toLocalTime().toNanoOfDay());

				// Check if result is after the 'until' date
				if (this.until != null && from.toLocalDate().isAfter(this.until)) {
					yield null;
				}
				yield from;

			}
			case WEEKLY -> {
				if (!this.byDay.isEmpty()) {
					// Adjust 'from' if the time of day has already passed
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
	}
}
