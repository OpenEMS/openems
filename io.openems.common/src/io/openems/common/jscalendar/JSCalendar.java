package io.openems.common.jscalendar;

import static com.google.common.base.MoreObjects.toStringHelper;
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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.ComparisonChain;
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
	public static class Tasks<PAYLOAD> {

		/**
		 * Returns a {@link JsonSerializer} for {@link Tasks}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Tasks<PAYLOAD>> serializer(JsonSerializer<PAYLOAD> payloadSerializer) {
			return serializer(Clock.systemDefaultZone(), payloadSerializer);
		}

		/**
		 * Returns a {@link JsonSerializer} for {@link Tasks}.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param clock             the {@link Clock}
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<Tasks<PAYLOAD>> serializer(Clock clock,
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<Tasks<PAYLOAD>>jsonArraySerializer(json -> {
				return new Tasks<PAYLOAD>(clock, json.getAsImmutableList(Task.serializer(payloadSerializer)));
			}, obj -> {
				return Task.serializer(payloadSerializer).toImmutableListSerializer().serialize(obj.tasks);
			});
		}

		/**
		 * Parse a List of {@link Task}s from a String representing a {@link JsonArray}
		 * - includes checks for null and empty.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param clock             the {@link Clock}
		 * @param string            the {@link JsonArray} string
		 * @param payloadSerializer a {@link JsonSerializer} for a Payload
		 * @return the {@link Tasks} object
		 */
		public static <PAYLOAD> Tasks<PAYLOAD> fromStringOrEmpty(Clock clock, String string,
				JsonSerializer<PAYLOAD> payloadSerializer) {
			if (string == null || string.isBlank()) {
				return Tasks.empty();
			}
			try {
				return Tasks.serializer(clock, payloadSerializer) //
						.deserialize(string);
			} catch (IllegalStateException | OpenemsNamedException e) {
				e.printStackTrace();
				return Tasks.empty();
			}
		}

		/**
		 * Parse a List of {@link Task}s without Payload from a String representing a
		 * {@link JsonArray} - includes checks for null and empty.
		 * 
		 * @param string the {@link JsonArray} string
		 * @return the {@link Tasks} object
		 */
		public static Tasks<Void> fromStringOrEmpty(String string) {
			return fromStringOrEmpty(Clock.systemDefaultZone(), string);
		}

		/**
		 * Parse a List of {@link Task}s without Payload from a String representing a
		 * {@link JsonArray} - includes checks for null and empty.
		 * 
		 * @param clock  the {@link Clock}
		 * @param string the {@link JsonArray} string
		 * @return the {@link Tasks} object
		 */
		public static Tasks<Void> fromStringOrEmpty(Clock clock, String string) {
			return fromStringOrEmpty(clock, string, VOID_SERIALIZER);
		}

		/**
		 * Parse a List of {@link Task}s from a String representing a {@link JsonArray}
		 * - includes checks for null and empty.
		 * 
		 * @param <PAYLOAD>         the type of the Payload
		 * @param string            the {@link JsonArray} string
		 * @param payloadSerializer a {@link JsonSerializer} for a Payload
		 * @return the {@link Tasks} object
		 */
		public static <PAYLOAD> Tasks<PAYLOAD> fromStringOrEmpty(String string,
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return fromStringOrEmpty(Clock.systemDefaultZone(), string, payloadSerializer);
		}

		/**
		 * Creates an empty {@link Tasks} object.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @return the {@link Tasks} object
		 */
		public static <PAYLOAD> Tasks<PAYLOAD> empty() {
			return new Tasks<PAYLOAD>(ImmutableList.of());
		}

		public static class Builder<PAYLOAD> {
			private final ImmutableList.Builder<Task<PAYLOAD>> tasks = ImmutableList.builder();
			private Clock clock = Clock.systemDefaultZone();

			protected Builder() {
			}

			public Builder<PAYLOAD> setClock(Clock clock) {
				this.clock = clock;
				return this;
			}

			/**
			 * Adds a {@link JSCalendar.Task}.
			 * 
			 * @param task a Consumer for {@link JSCalendar.Task.Builder}
			 * @return myself
			 */
			public Builder<PAYLOAD> add(Consumer<Task.Builder<PAYLOAD>> task) {
				var t = Task.<PAYLOAD>create();
				task.accept(t);
				this.tasks.add(t.build());
				return this;
			}

			public Tasks<PAYLOAD> build() {
				return new Tasks<PAYLOAD>(this.clock, this.tasks.build());
			}
		}

		/**
		 * Create a {@link Tasks} {@link Builder}.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @return a {@link Builder}
		 */
		public static <PAYLOAD> Builder<PAYLOAD> create() {
			return new Builder<PAYLOAD>();
		}

		public final ImmutableList<Task<PAYLOAD>> tasks;

		private final Clock clock;
		private final TreeSet<OneTask<PAYLOAD>> oneTasks;

		private OneTask<PAYLOAD> lastActiveOneTask = null;

		private Tasks(ImmutableList<Task<PAYLOAD>> tasks) {
			this(Clock.systemDefaultZone(), tasks);
		}

		private Tasks(Clock clock, ImmutableList<Task<PAYLOAD>> tasks) {
			this.clock = clock;
			this.tasks = tasks;

			var now = ZonedDateTime.now(this.clock);
			var oneTasks = this._getOneTasksBetween(now, now.plusDays(1));
			this.oneTasks = oneTasks.isEmpty() //
					? null // will never have any OneTasks
					: oneTasks;
		}

		/**
		 * Gets the currently active {@link OneTask}.
		 * 
		 * @return active {@link OneTask}; null if no {@link OneTask} is active
		 */
		public synchronized OneTask<PAYLOAD> getActiveOneTask() {
			final OneTask<PAYLOAD> result;

			if (this.oneTasks == null) {
				result = null; // will never have any OneTasks

			} else {
				final var wasEmpty = this.oneTasks.isEmpty();
				final var now = ZonedDateTime.now(this.clock);
				if (wasEmpty) { // Refill empty list
					this.oneTasks.addAll(this._getOneTasksBetween(now, now.plusDays(1)));
				}

				if (this.oneTasks.isEmpty()) {
					result = null; // Still no OneTasks

				} else {
					var first = this.oneTasks.getFirst();
					if (!first.end.isAfter(now)) { // END is exclusive
						this.oneTasks.removeFirst(); // Remove outdated OneTasks
						if (wasEmpty) {
							result = first; // make sure every OneTask is returned at least once
						} else {
							result = this.getActiveOneTask(); // get next OneTask
						}

					} else if (first.start.isAfter(now)) { // START is inclusive
						result = null; // not active yet

					} else {
						result = first; // currently active
					}
				}
			}

			this.lastActiveOneTask = result;
			return result;
		}

		/**
		 * Gets the last result of {@link #getActiveOneTask()}.
		 *
		 * @return {@link OneTask} or null
		 */
		public synchronized OneTask<PAYLOAD> getLastActiveOneTask() {
			return this.lastActiveOneTask;
		}

		/**
		 * Gets the occurences of the {@link Task}s (including currently active task)
		 * between two dates.
		 * 
		 * @param from the from timestamp
		 * @param to   the to timestamp
		 * @return a list of {@link OneTask}s
		 */
		public TreeSet<OneTask<PAYLOAD>> getOneTasksBetween(ZonedDateTime from, ZonedDateTime to) {
			return this._getOneTasksBetween(from, to);
		}

		/**
		 * Gets the number of {@link JSCalendar.Task}s.
		 * 
		 * @return count
		 */
		public int numberOfTasks() {
			return this.tasks.size();
		}

		protected boolean tasksIsEmpty() {
			return this.tasks.isEmpty();
		}

		private TreeSet<OneTask<PAYLOAD>> _getOneTasksBetween(ZonedDateTime from, ZonedDateTime to) {
			final var result = new TreeSet<OneTask<PAYLOAD>>();
			for (var task : this.tasks) {
				for (var occurence : task.getOccurencesBetween(from, to)) {
					final var occurenceStart = occurence.isBefore(from) //
							? from //
							: occurence;
					var occurenceEnd = task.duration == null //
							? occurenceStart //
							: occurence.plus(task.duration);
					if (occurenceEnd.isAfter(to)) {
						occurenceEnd = to;
					}
					addToOccurencesBetween(result, task, occurenceStart, occurenceEnd);
				}
			}

			mergeOneTasksOfSameType(result);

			return result;
		}

		/**
		 * Holds data of one task.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 */
		public static record OneTask<PAYLOAD>(Task<PAYLOAD> parentTask, ZonedDateTime start, Duration duration,
				ZonedDateTime end) implements Comparable<OneTask<PAYLOAD>> {

			/**
			 * Builds a {@link OneTask}.
			 * 
			 * @param <PAYLOAD>  the type of the Payload
			 * @param parentTask the parent {@link Task}
			 * @param start      the start timestamp
			 * @param duration   the {@link Duration}
			 * @return the {@link OneTask}
			 */
			public static <PAYLOAD> OneTask<PAYLOAD> from(Task<PAYLOAD> parentTask, ZonedDateTime start,
					Duration duration) {
				return new OneTask<PAYLOAD>(parentTask, start, duration,
						duration == null ? null : start.plus(duration));
			}

			/**
			 * Builds a {@link OneTask}.
			 * 
			 * @param <PAYLOAD>  the type of the Payload
			 * @param parentTask the parent {@link Task}
			 * @param start      the start timestamp
			 * @param end        the end timestamp
			 * @return the {@link OneTask}
			 */
			public static <PAYLOAD> OneTask<PAYLOAD> from(Task<PAYLOAD> parentTask, ZonedDateTime start,
					ZonedDateTime end) {
				return new OneTask<PAYLOAD>(parentTask, start, //
						start.isEqual(end) ? null : Duration.between(start, end), end);
			}

			@Override
			public final String toString() {
				return toStringHelper(OneTask.class) //
						.add("start", this.start) //
						.add("end", this.end) //
						.add("duration", this.duration) //
						.add("payload", this.parentTask.payload) //
						.toString();
			}

			@Override
			public int compareTo(OneTask<PAYLOAD> o) {
				return ComparisonChain.start() //
						.compare(this.start, o.start) //
						.compare(this.end, o.end) //
						.compare(this.parentTask.toString(), o.parentTask.toString()) //
						.result();
			}

			/**
			 * Gets the payload inherited from the parent {@link Task}.
			 * 
			 * @return payload
			 */
			public PAYLOAD payload() {
				return this.parentTask.payload;
			}
		}

		// Recursively adds OneTasks to results
		private static <PAYLOAD> void addToOccurencesBetween(TreeSet<OneTask<PAYLOAD>> result, Task<PAYLOAD> task,
				ZonedDateTime occurenceStart, ZonedDateTime occurenceEnd) {
			// Last OneTask in results before this occurence
			var lastBefore = result.descendingSet().stream() //
					.filter(ot -> !occurenceStart.isBefore(ot.start)) //
					.findFirst();
			final ZonedDateTime start = lastBefore //
					.map(ot -> ot.end.isAfter(occurenceStart) //
							? ot.end //
							: occurenceStart) //
					.orElse(occurenceStart);

			// First OneTask in results after this occurence
			var firstAfter = result.stream() //
					.filter(ot -> ot.duration != null //
							? !start.isAfter(ot.start) //
							: start.isBefore(ot.start)) //
					.findFirst();
			final ZonedDateTime end = firstAfter //
					.map(ot -> ot.start.isBefore(occurenceEnd) //
							? ot.start //
							: occurenceEnd) //
					.orElse(occurenceEnd);

			if (task.duration != null && start.isEqual(end)) {
				// This is a Task with Duration, but during creation of OneTasks for the last
				// one start would be same as end -> do not add to result
			} else {
				// Add to result
				result.add(OneTask.<PAYLOAD>from(task, start, end));
			}

			if (!occurenceEnd.isEqual(end)) {
				// Recursive call to re-distribute remaining Task#
				var nextOccurenceStart = firstAfter.map(OneTask::end) //
						.orElse(end);

				addToOccurencesBetween(result, task, nextOccurenceStart, occurenceEnd);
			}
		}

		private static <PAYLOAD> void mergeOneTasksOfSameType(TreeSet<OneTask<PAYLOAD>> result) {
			boolean retry;
			do {
				retry = false;
				var toAdd = new ArrayList<OneTask<PAYLOAD>>();
				var toRemove = new ArrayList<OneTask<PAYLOAD>>();

				for (var t0 : result) {
					var t1 = result.higher(t0);
					if (t1 == null) {
						continue; // last element
					}
					if (t0.parentTask.equals(t1.parentTask) && t0.end.isEqual(t1.start)) {
						// Merge OneTasks
						toRemove.add(t0);
						toRemove.add(t1);
						toAdd.add(OneTask.from(t0.parentTask, t0.start, t1.end));
						retry = true;
						break;
					}
				}

				result.removeAll(toRemove); // avoids ConcurrentModificationException
				result.addAll(toAdd);
			} while (retry);
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
		 * Create a {@link Task} {@link Builder}.
		 * 
		 * @param <PAYLOAD> the type of the Payload
		 * @return a {@link Builder}
		 */
		public static <PAYLOAD> Builder<PAYLOAD> create() {
			return new Builder<PAYLOAD>();
		}

		/**
		 * Gets the occurences of the {@link Task} (including currently active task)
		 * between two dates.
		 * 
		 * <p>
		 * If no occurence exists between the dates, returns the earliest occurence
		 * afterwards.
		 * 
		 * <p>
		 * If no occurence exists, not even afterwards, an empty list is returned.
		 * 
		 * @param from the from timestamp
		 * @param to   the to timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public ImmutableList<ZonedDateTime> getOccurencesBetween(ZonedDateTime from, ZonedDateTime to) {
			var result = new ArrayList<ZonedDateTime>();
			for (var rr : this.recurrenceRules) {
				var nextFrom = this.duration == null //
						? from //
						: from.minus(this.duration); // query active tasks;
				while (true) {
					var start = rr.getNextOccurence(this.start, nextFrom);
					if (start == null) {
						break; // impossible occurence
					}
					if (start.isAfter(to) && !result.isEmpty()) {
						break; // at least one result; even if its out of range
					}
					result.add(start);
					nextFrom = start.plusNanos(1);
				}
			}
			return ImmutableList.copyOf(result);
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
		// NOTE: "until" is 'inclusive'
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
				Objects.requireNonNull(this.frequency);

				var byDay = this.byDay.build();
				switch (this.frequency) {
				case DAILY, MONTHLY, YEARLY -> {
					if (!byDay.isEmpty()) {
						System.err.println("WARNING: RecurrenceRule with Frequency " + this.frequency
								+ " is incomaptible with byDay " + byDay);
					}
				}
				case WEEKLY -> {
					if (byDay.isEmpty()) {
						// If no DayOfWeek are given: add all by default
						byDay = ImmutableSortedSet.copyOf(DayOfWeek.values());
					}
				}
				}
				return new RecurrenceRule(this.frequency, this.until, byDay);
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
		 * Gets the occurences of the {@link RecurrenceRule} between two dates. If no
		 * occurence exists between the dates, returns the earliest occurence
		 * afterwards.
		 * 
		 * @param taskStart the start of the {@link Task}
		 * @param from      the from timestamp
		 * @param to        the to timestamp
		 * @return a {@link ZonedDateTime}
		 */
		protected ImmutableList<ZonedDateTime> getOccurencesBetween(LocalDateTime taskStart, ZonedDateTime from,
				ZonedDateTime to) {
			var result = new ArrayList<ZonedDateTime>();
			while (true) {
				var time = this.getNextOccurence(taskStart, from);
				if (time.isAfter(to) && !result.isEmpty()) {
					break;
				}
				result.add(time);
				from = time.plusNanos(1);
			}
			return ImmutableList.copyOf(result);
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

			final var result = switch (this.frequency) {
			case DAILY -> {
				// Adjust 'from' if the time of day has already passed
				if (from.toLocalTime().isAfter(taskStart.toLocalTime())) {
					from = from.plusDays(1); // tomorrow
				}
				yield from.with(NANO_OF_DAY, taskStart.toLocalTime().toNanoOfDay());
			}

			case WEEKLY -> {
				// this.byDay is guaranteed to be never empty
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
				yield from.with(nextOrSame(nextByDay)); // first day in list
			}
			case MONTHLY -> null; // not implemented
			case YEARLY -> null; // not implemented
			};

			// Check if result is after the 'until' date
			if (this.until != null && result.toLocalDate().isAfter(this.until)) {
				return null;
			}
			return result;
		}
	}
}
