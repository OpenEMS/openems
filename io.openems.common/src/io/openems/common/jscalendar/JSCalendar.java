package io.openems.common.jscalendar;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.FunctionUtils.doNothing;
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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
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

	private static final Logger LOG = LoggerFactory.getLogger(JSCalendar.class);
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
			if (string == null || string.isBlank() || string.equals("[]")) {
				return Tasks.empty();
			}
			try {
				return Tasks.serializer(clock, payloadSerializer) //
						.deserialize(string);
			} catch (Exception e) {
				LOG.error("Unable to parse Tasks from " + string + ": " + e.getMessage());
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
			 * @param task a Function for {@link JSCalendar.Task.Builder}
			 * @return myself
			 */
			public Builder<PAYLOAD> add(Function<Task.Builder<PAYLOAD>, Task.Builder<PAYLOAD>> task) {
				var t = Task.<PAYLOAD>create();
				this.tasks.add(task.apply(t).build());
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
				final var now = ZonedDateTime.now(this.clock);

				if (this.oneTasks.size() <= 1) {
					// Last entry can have the wrong end-timestamp, so we clear it and refill the
					// list with at least two OneTasks.
					// There are corner-cases (e.g. recurrence with until) where this code gets
					// executed on every Cycle.
					this.oneTasks.clear();
					var ots = this._getOneTasksBetween(now, now.plusDays(1));
					this.oneTasks.addAll(ots);
					if (ots.size() == 1) {
						// If only one OneTask was added, try to add even more to avoid coming here
						// every cycle
						final var ot = ots.getLast();
						final var start = ot.duration.isZero() //
								? ot.end.plusNanos(1) //
								: ot.end;
						var moreOts = this._getOneTasksBetween(start, ot.end.plusDays(1));
						this.oneTasks.addAll(moreOts);
					}
				}

				if (this.oneTasks.isEmpty()) {
					result = null; // Still no OneTasks

				} else {
					var first = this.oneTasks.getFirst();
					if (now.isBefore(first.start)) { // START is inclusive
						result = null; // not active yet

					} else if (first != this.lastActiveOneTask) {
						// make sure every OneTask is returned at least once
						result = first;

					} else if (now.isBefore(first.end)) {
						result = first; // currently active

					} else { // END is exclusive
						this.oneTasks.removeFirst(); // Remove outdated OneTasks
						result = this.getActiveOneTask();
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
		 * Gets the occurrences of the {@link Task}s (including currently active task)
		 * between two dates.
		 *
		 * @param from the 'from' timestamp
		 * @param to   the to timestamp
		 * @return a list of {@link OneTask}s
		 */
		public OneTasks<PAYLOAD> getOneTasksBetween(ZonedDateTime from, ZonedDateTime to) {
			return OneTasks.from(this._getOneTasksBetween(from, to));
		}

		/**
		 * Gets the number of {@link JSCalendar.Task}s.
		 *
		 * @return count
		 */
		public int numberOfTasks() {
			return this.tasks.size();
		}

		/**
		 * Returns a new {@link Tasks} object with the specified {@link Task} added.
		 *
		 * @param newTask the {@link Task} to add
		 * @return a new {@link Tasks} instance including the added task
		 */
		public Tasks<PAYLOAD> withAddedTask(Task<PAYLOAD> newTask) {
			var updatedTasks = new ImmutableList.Builder<Task<PAYLOAD>>()//
					.addAll(this.tasks)//
					.add(newTask.withUpdatedNow(this.clock))//
					.build();
			return new Tasks<>(this.clock, updatedTasks);
		}

		/**
		 * Returns a new {@link Tasks} object with the specified {@link Task} updated.
		 *
		 * @param updatedTask the {@link Task} to update
		 * @return a new {@link Tasks} instance with the task replaced
		 * @throws IllegalArgumentException if no task with the given UUID exists
		 */
		public Tasks<PAYLOAD> withUpdatedTask(Task<PAYLOAD> updatedTask) {
			if (this.tasks.stream().noneMatch(t -> t.uid().equals(updatedTask.uid()))) {
				throw new IllegalArgumentException("No task found with UUID " + updatedTask.uid());
			}

			var updatedTasks = this.tasks.stream()//
					.map(task -> task.uid().equals(updatedTask.uid()) //
							? updatedTask.withUpdatedNow(this.clock) //
							: task)//
					.collect(toImmutableList());

			return new Tasks<>(this.clock, updatedTasks);
		}

		/**
		 * Returns a new {@link Tasks} object with the {@link Task} having the specified
		 * UUID removed.
		 *
		 * @param uid the {@link UUID} of the {@link Task} to remove
		 * @return a new {@link Tasks} instance without the specified task
		 */
		public Tasks<PAYLOAD> withRemovedTask(UUID uid) {
			if (this.tasks.stream().noneMatch(t -> t.uid().equals(uid))) {
				throw new IllegalArgumentException("No task found with UUID " + uid);
			}

			var updatedTasks = this.tasks.stream()//
					.filter(task -> !task.uid().equals(uid))//
					.collect(toImmutableList());

			if (updatedTasks.size() == this.tasks.size()) {
				return this;
			}

			return new Tasks<>(this.clock, updatedTasks);
		}

		/**
		 * Gets these {@link Tasks} as {@link JsonArray}.
		 *
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return {@link JsonArray}
		 */
		public JsonArray toJson(JsonSerializer<PAYLOAD> payloadSerializer) {
			if (this.tasks.isEmpty()) {
				return new JsonArray();
			}
			return (JsonArray) JSCalendar.Tasks.serializer(payloadSerializer).serialize(this);
		}

		private TreeSet<OneTask<PAYLOAD>> _getOneTasksBetween(ZonedDateTime from, ZonedDateTime to) {
			final var result = new TreeSet<OneTask<PAYLOAD>>();
			for (var task : this.tasks) {
				for (var occurrence : task.getOccurrencesBetween(from, to)) {
					final var occurrenceStart = occurrence.isBefore(from) //
							? from //
							: occurrence;
					var occurenceEnd = task.duration == Duration.ZERO //
							? occurrenceStart //
							: occurrence.plus(task.duration);
					if (occurenceEnd.isAfter(to) && occurrenceStart.isBefore(to)) {
						occurenceEnd = to;
					}
					addToOccurrencesBetween(result, task, occurrenceStart, occurenceEnd);
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

			public OneTask {
				Objects.requireNonNull(parentTask, "parentTask cannot be null");
				Objects.requireNonNull(start, "start cannot be null");
				Objects.requireNonNull(duration, "duration cannot be null");
				Objects.requireNonNull(end, "end cannot be null");
				if (!start.plus(duration).isEqual(end)) {
					throw new IllegalArgumentException(
							"Start, Duration and End are not matching: " + start + ", " + duration + ", " + end);
				}
			}

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
				Objects.requireNonNull(start, "duration cannot be null");
				return new OneTask<PAYLOAD>(parentTask, start, duration, start.plus(duration));
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
						start.isEqual(end) //
								? Duration.ZERO //
								: Duration.between(start, end),
						end);
			}

			/**
			 * Returns a {@link JsonSerializer} for a {@link OneTask} without payload.
			 *
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<OneTask<Void>> serializer() {
				return serializer(VOID_SERIALIZER);
			}

			/**
			 * Returns a {@link JsonSerializer} for a {@link OneTask}.
			 *
			 * @param <PAYLOAD>         the type of the Payload
			 * @param payloadSerializer a {@link JsonSerializer} for the Payload
			 * @return the created {@link JsonSerializer}
			 */
			public static <PAYLOAD> JsonSerializer<OneTask<PAYLOAD>> serializer(
					JsonSerializer<PAYLOAD> payloadSerializer) {
				return JsonSerializerUtil.<OneTask<PAYLOAD>>jsonObjectSerializer(json -> {
					var uid = json.getUuidOrNull("uid");
					var start = json.getZonedDateTime("start");
					var end = json.getOptionalZonedDateTime("end") //
							.orElse(start);
					var duration = json.getOptionalDuration("duration") //
							.orElse(Duration.ZERO);
					var payload = json.getObjectOrNull("payload", payloadSerializer);
					var task = new Task<PAYLOAD>(uid, null, start.toLocalDateTime(), duration, ImmutableList.of(),
							payload);
					return new OneTask<PAYLOAD>(task, start, duration, end);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("uid", obj.parentTask.uid) //
							.addProperty("start", obj.start) //
							.addProperty("duration", obj.duration) //
							.addProperty("end", obj.end) //
							.onlyIf(obj.payload() != null, //
									j -> j.add("payload", payloadSerializer.serialize(obj.payload()))) //
							.build();
				});
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
		private static <PAYLOAD> void addToOccurrencesBetween(TreeSet<OneTask<PAYLOAD>> result, Task<PAYLOAD> task,
				ZonedDateTime occurrenceStart, ZonedDateTime occurrenceEnd) {
			// Last OneTask in results before this occurence
			var lastBefore = result.descendingSet().stream() //
					.filter(ot -> !occurrenceStart.isBefore(ot.start)) //
					.findFirst();
			final ZonedDateTime start = lastBefore //
					.map(ot -> ot.end.isAfter(occurrenceStart) //
							? ot.end //
							: occurrenceStart) //
					.orElse(occurrenceStart);

			// First OneTask in results after this occurence
			var firstAfter = result.stream() //
					.filter(ot -> ot.duration == Duration.ZERO //
							? start.isBefore(ot.start) //
							: !start.isAfter(ot.start)) //
					.findFirst();
			final ZonedDateTime end = firstAfter //
					.map(ot -> ot.start.isBefore(occurrenceEnd) //
							? ot.start //
							: occurrenceEnd) //
					.orElse(occurrenceEnd);

			if (task.duration != Duration.ZERO && start.isEqual(end)) {
				// This is a Task with Duration, but during creation of OneTasks for the last
				// one start would be same as end -> do not add to result
			} else {
				// Add to result
				result.add(OneTask.<PAYLOAD>from(task, start, end));
			}

			if (!occurrenceEnd.isEqual(end)) {
				// Recursive call to re-distribute remaining Task#
				var nextOccurrenceStart = firstAfter.map(OneTask::end) //
						.orElse(end);

				addToOccurrencesBetween(result, task, nextOccurrenceStart, occurrenceEnd);
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

	/**
	 * Helper utilities to handle lists of {@link OneTask}s.
	 */
	public static class OneTasks<PAYLOAD> implements Iterable<OneTask<PAYLOAD>> {

		/**
		 * Returns a {@link JsonSerializer} for {@link OneTasks}.
		 *
		 * @param <PAYLOAD>         the type of the Payload
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<OneTasks<PAYLOAD>> serializer(
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return serializer(Clock.systemDefaultZone(), payloadSerializer);
		}

		/**
		 * Returns a {@link JsonSerializer} for {@link OneTasks}.
		 *
		 * @param <PAYLOAD>         the type of the Payload
		 * @param clock             the {@link Clock}
		 * @param payloadSerializer a {@link JsonSerializer} for the Payload
		 * @return the created {@link JsonSerializer}
		 */
		public static <PAYLOAD> JsonSerializer<OneTasks<PAYLOAD>> serializer(Clock clock,
				JsonSerializer<PAYLOAD> payloadSerializer) {
			return JsonSerializerUtil.<OneTasks<PAYLOAD>>jsonArraySerializer(json -> {
				return new OneTasks<PAYLOAD>(
						json.getAsImmutableSortedSet(OneTask.serializer(payloadSerializer), Ordering.natural()));
			}, obj -> {
				var s = OneTask.serializer(payloadSerializer);
				return obj.oneTasks.stream() //
						.map(s::serialize) //
						.collect(toJsonArray());
			});
		}

		private static <PAYLOAD> OneTasks<PAYLOAD> from(TreeSet<OneTask<PAYLOAD>> oneTasks) {
			return new OneTasks<>(ImmutableSortedSet.copyOf(oneTasks));
		}

		private final ImmutableSortedSet<OneTask<PAYLOAD>> oneTasks;

		private OneTasks(ImmutableSortedSet<OneTask<PAYLOAD>> oneTasks) {
			this.oneTasks = oneTasks;
		}

		/**
		 * Gets the Payload for the given time.
		 *
		 * @param time the {@link ZonedDateTime}
		 * @return the Payload; null if no {@link OneTask} is existing at the given time
		 */
		public PAYLOAD getPayloadAt(ZonedDateTime time) {
			return this.oneTasks.stream() //
					.filter(ot -> {
						boolean startsBeforeOrAtTime = !ot.start.isAfter(time);
						boolean strictlyBeforeEnd = time.isBefore(ot.end);
						return startsBeforeOrAtTime && strictlyBeforeEnd;
					}) //
					.findFirst() //
					.map(OneTask::payload) //
					.orElse(null);
		}

		/**
		 * Gets a {@link Stream} of {@link OneTasks} between from (inclusive) and to
		 * (exclusive).
		 *
		 * @param from the from {@link ZonedDateTime}
		 * @param to   the to {@link ZonedDateTime}
		 * @return a Stream of values; possibly empty
		 */
		public final Stream<OneTask<PAYLOAD>> getBetween(ZonedDateTime from, ZonedDateTime to) {
			return this.oneTasks.stream() //
					.filter(ot -> {
						boolean endsAfterFrom = ot.end.isAfter(from);
						boolean startsAtOrBeforeTo = ot.start.isBefore(to);
						return endsAfterFrom && startsAtOrBeforeTo;
					});
		}

		@Override
		public Iterator<OneTask<PAYLOAD>> iterator() {
			return this.oneTasks.iterator();
		}

		/**
		 * {@link ImmutableSortedSet#size()}.
		 *
		 * @return the number of elements in this collection
		 */
		public int size() {
			return this.oneTasks.size();
		}

		/**
		 * {@link ImmutableSortedSet#isEmpty()}.
		 *
		 * @return {@code true} if this collection contains no elements
		 */
		public boolean isEmpty() {
			return this.oneTasks.isEmpty();
		}
	}

	public static record Task<PAYLOAD>(UUID uid, ZonedDateTime updated, LocalDateTime start, Duration duration,
			ImmutableList<RecurrenceRule> recurrenceRules, PAYLOAD payload) {

		public Task {
			Objects.requireNonNull(uid, "uid cannot be null");
			Objects.requireNonNull(start, "start cannot be null");
			Objects.requireNonNull(duration, "duration cannot be null");
			Objects.requireNonNull(recurrenceRules, "recurrenceRules cannot be null");
		}

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
						.setStart(json.getStringOrNull("start")) //
						.setDuration(json.getDurationOrNull("duration")) //
						.setPayload(json.getObjectOrNull(PROPERTY_PAYLOAD, payloadSerializer));

				json.getOptionalList("recurrenceRules", RecurrenceRule.serializer()) //
						.orElse(emptyList()) //
						.forEach(b::addRecurrenceRule);

				return b.build();
			}, obj -> {
				return buildJsonObject() //
						.addProperty("@type", "Task") //
						.addProperty("uid", obj.uid) //
						.onlyIf(obj.updated != null, //
								j -> j.addProperty("updated", obj.updated)) //
						.addProperty("start", LocalDate.from(obj.start).equals(EPOCH) //
								? obj.start.format(ISO_LOCAL_TIME) //
								: obj.start.format(ISO_LOCAL_DATE_TIME)) //
						.addProperty("duration", obj.duration) //
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

			public Builder<PAYLOAD> setStart(String start) throws DateTimeParseException {
				if (start == null) {
					return this.setStart((LocalDateTime) null);
				}
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
				return this.setDuration(duration == null //
						? null //
						: Duration.parse(duration));
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
				final var uid = this.uid == null //
						? UUID.randomUUID() //
						: this.uid;
				var recurrenceRules = this.recurrenceRules.build();
				var duration = this.duration == null //
						? Duration.ZERO //
						: this.duration;

				if (this.start == null && duration == Duration.ZERO && recurrenceRules.isEmpty()
						&& this.payload != null) {
					// Consider this is a default/fallback task that is always active
					this.start = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN);
					duration = Duration.ofDays(1);
					recurrenceRules = ImmutableList.of(RecurrenceRule.create() //
							.setFrequency(RecurrenceFrequency.DAILY) //
							.build());
				}

				return new Task<PAYLOAD>(uid, this.updated, this.start, duration, recurrenceRules, this.payload);
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
		 * Gets the occurrences of the {@link Task} (including currently active task)
		 * between two dates.
		 *
		 * <p>
		 * If no occurrence exists between the dates, returns the earliest occurrence
		 * afterwards.
		 *
		 * <p>
		 * If no occurrence exists, not even afterwards, an empty list is returned.
		 *
		 * @param from the 'from' timestamp
		 * @param to   the to timestamp
		 * @return a {@link ZonedDateTime}
		 */
		public ImmutableList<ZonedDateTime> getOccurrencesBetween(ZonedDateTime from, ZonedDateTime to) {
			var result = new ArrayList<ZonedDateTime>();
			for (var rr : this.recurrenceRules) {
				var nextFrom = this.duration == Duration.ZERO //
						? from //
						: from.minus(this.duration); // query active tasks;
				while (true) {
					var start = rr.getNextOccurrence(this.start, nextFrom);
					if (start == null) {
						break; // impossible occurence
					}
					if (start.isAfter(to) && !result.isEmpty()) {
						break; // at least one result; even if it's out of range
					}
					result.add(start);
					nextFrom = start.plusNanos(1);
				}
			}
			return ImmutableList.copyOf(result);
		}

		/**
		 * Returns a new {@link Task} with the {@code updated} field set to the current
		 * time according to the provided {@link Clock}.
		 *
		 * @param clock the {@link Clock} to use for the current time
		 * @return a new {@link Task} instance with the updated timestamp
		 */
		public Task<PAYLOAD> withUpdatedNow(Clock clock) {
			return new Task<>(//
					this.uid, //
					ZonedDateTime.now(clock), //
					this.start, //
					this.duration, //
					this.recurrenceRules, //
					this.payload);
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

	/**
	 * According to the JSCalendar documentation, a NDay represents a day of the
	 * week and an optional nth occurrence within a period.
	 * 
	 * @param day         the {@link DayOfWeek} (e.g., MONDAY, TUESDAY, etc.)
	 * @param nthOfPeriod the occurrence number within the period (e.g., 1 for
	 *                    first, -1 for last). Can be null.
	 */
	public record NDay(DayOfWeek day, Integer nthOfPeriod) {
		public NDay {
			if (nthOfPeriod != null && (nthOfPeriod == 0 || nthOfPeriod < -5 || nthOfPeriod > 5)) {
				throw new IllegalArgumentException("nthOfPeriod must be between -5 and 5 (excluding 0)");
			}
		}
	}

	public record RecurrenceRule(RecurrenceFrequency frequency, LocalDate until, ImmutableSortedSet<NDay> byDay) {
		// TODO "until" is defined as LocalDateTime in the RFC
		// NOTE: "until" is 'inclusive'
		// https://www.rfc-editor.org/rfc/rfc8984.html#section-4.3.3

		/**
		 * Returns a {@link JsonSerializer} for a {@link RecurrenceRule}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<RecurrenceRule> serializer() {
			return jsonObjectSerializer(RecurrenceRule.class, json -> //
			new RecurrenceRule(//
					json.getEnum("frequency", RecurrenceFrequency.class), //
					json.getOptionalLocalDate("until").orElse(null), //
					json.getNullableJsonArrayPath("byDay") //
							.mapToOptional(arr -> arr.getAsImmutableSortedSet(//
									RecurrenceRule::deserializeByDayElement, //
									Comparator.comparing(NDay::day) //
							)).orElse(ImmutableSortedSet.of())), //
					obj -> buildJsonObject() //
							.addPropertyIfNotNull("frequency", obj.frequency().name().toLowerCase()) //
							.addPropertyIfNotNull("until", obj.until()) //
							.onlyIf(!obj.byDay.isEmpty(), j -> {
								if (obj.byDay.stream().allMatch(nd -> nd.nthOfPeriod() == null)) {
									j.add("byDay", dayOfWeekSerializer().toSetSerializer().serialize(//
											obj.byDay.stream()//
													.map(NDay::day)//
													.collect(ImmutableSortedSet.toImmutableSortedSet(//
															Comparator.naturalOrder()//
									))));
								} else {
									j.add("byDay", nDaySerializer().toSetSerializer().serialize(obj.byDay));
								}
							}).build());
		}

		private static final Map<String, DayOfWeek> STRING_TO_DAY = Map.of(//
				"mo", MONDAY, //
				"tu", TUESDAY, //
				"we", WEDNESDAY, //
				"th", THURSDAY, //
				"fr", FRIDAY, //
				"sa", SATURDAY, //
				"su", SUNDAY);

		private static final Map<DayOfWeek, String> DAY_TO_STRING = //
				STRING_TO_DAY.entrySet()//
						.stream()//
						.collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

		private static JsonSerializer<DayOfWeek> dayOfWeekSerializer() {
			return jsonSerializer(//
					DayOfWeek.class, //
					json -> json.getAsStringParsed(//
							RecurrenceRule::dayOfWeekEnumConverter, //
							() -> new StringParser.ExampleValues<>("mo", MONDAY)//
					), //
					obj -> new JsonPrimitive(dayOfWeekStringConverter(obj))//
			);
		}

		protected static NDay deserializeByDayElement(JsonElementPath el) {
			// Object -> {"day":"mo","nthOfPeriod":1}
			if (el.isJsonObject()) {
				return nDaySerializer().deserializePath(el);
			} else {
				// String -> "mo"
				return new NDay(dayOfWeekEnumConverter(el.getAsString()), null);
			}

		}

        protected static JsonSerializer<NDay> nDaySerializer() {
            return jsonObjectSerializer(NDay.class, json -> //
                    new NDay(//
                            dayOfWeekEnumConverter(json.getString("day")), //
                            json.getOptionalInt("nthOfPeriod").orElse(null) //
                    ), obj -> {
                // If nthOfPeriod null serialize as String
                if (obj.nthOfPeriod() == null) {
                    return new JsonPrimitive(dayOfWeekStringConverter(obj.day));
                }
                return buildJsonObject() //
                        .addProperty("day", dayOfWeekStringConverter(obj.day)) //
                        .addProperty("nthOfPeriod", obj.nthOfPeriod()) //
                        .build();
            });
        }

		/**
		 * Converts a string to {@link DayOfWeek}. In our implementation, we use for
		 * example 'su' for Sunday as JSON property for 'day'. Therefore, a conversion
		 * to {@link DayOfWeek} is required.
		 *
		 * @param element the element e.g. 'su'
		 * @return a {@link DayOfWeek} instance e.g. {@link DayOfWeek#SUNDAY}
		 */
		private static DayOfWeek dayOfWeekEnumConverter(String element) {
			var day = STRING_TO_DAY.get(element);
			if (day == null) {
				throw new OpenemsRuntimeException("Unable to parse value '" + element + "' to DayOfWeek");
			}
			return day;
		}

		private static String dayOfWeekStringConverter(DayOfWeek dow) {
			return DAY_TO_STRING.get(dow);
		}

		public static class Builder {
			private RecurrenceFrequency frequency;
			private LocalDate until;
			private ImmutableSortedSet.Builder<NDay> byDay = ImmutableSortedSet
					.orderedBy(Comparator.comparing(NDay::day));

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
			 * Adds a `byDay`rule. Adds one or more {@link NDay}s on which the task should
			 * repeat itself. For example day: 'su' for every sunday, nthOfPeriod: 3 for
			 * every third sunday.
			 *
			 * @param byDay the {@link NDay}s
			 * @return myself
			 */
			public Builder addByDay(NDay... byDay) {
				stream(byDay).forEach(this.byDay::add);
				return this;
			}

			/**
			 * Adds a `byDay`rule. Adds one or more weekdays on which the task should repeat
			 * itself. For example 'su' for every sunday.
			 *
			 * @param byDay the {@link DayOfWeek}s
			 * @return myself
			 */
			public Builder addByDay(DayOfWeek... byDay) {
				stream(byDay).forEach(d -> this.byDay.add(new NDay(d, null)));
				return this;
			}

			public RecurrenceRule build() {
				Objects.requireNonNull(this.frequency);

				var byDay = this.byDay.build();
				switch (this.frequency) {
				case DAILY, YEARLY -> {
					if (!byDay.isEmpty()) {
						LOG.warn("WARNING: RecurrenceRule with Frequency {} is incompatible with byDay {}",
								this.frequency, byDay);
					}
				}
				case MONTHLY -> doNothing();
				case WEEKLY -> {
					boolean hasNthSet = byDay.stream().anyMatch(d -> d.nthOfPeriod() != null);
					if (hasNthSet) {
						LOG.warn("WARNING: nthOfPeriod is not allowed for WEEKLY recurrence");
					}
					if (byDay.isEmpty()) {
						// if no day is given, add all weekdays by default
						byDay = Arrays.stream(DayOfWeek.values())//
								.map(d -> new NDay(d, null))//
								.collect(ImmutableSortedSet.toImmutableSortedSet(//
										Comparator.comparing(NDay::day)//
								));
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
		 * Gets the occurrences of the {@link RecurrenceRule} between two dates. If no
		 * occurrence exists between the dates, returns the earliest occurrence
		 * afterwards.
		 *
		 * @param taskStart the start of the {@link Task}
		 * @param from      the 'from' timestamp
		 * @param to        the to timestamp
		 * @return a {@link ZonedDateTime}
		 */
		protected ImmutableList<ZonedDateTime> getOccurrencesBetween(LocalDateTime taskStart, ZonedDateTime from,
				ZonedDateTime to) {
			var result = new ArrayList<ZonedDateTime>();
			while (true) {
				var time = this.getNextOccurrence(taskStart, from);
				if (time.isAfter(to) && !result.isEmpty()) {
					break;
				}
				result.add(time);
				from = time.plusNanos(1);
			}
			return ImmutableList.copyOf(result);
		}

		/**
		 * Gets the next occurrence of the {@link RecurrenceRule} at or after a date.
		 *
		 * @param taskStart the start timestamp of the {@link Task}
		 * @param from      the 'from' date
		 * @return a {@link ZonedDateTime}
		 */
		public ZonedDateTime getNextOccurrence(LocalDateTime taskStart, ZonedDateTime from) {
			final var taskStartZoned = taskStart.atZone(from.getZone());
			from = from.isBefore(taskStartZoned) //
					? taskStartZoned //
					: from;

			final var result = switch (this.frequency) {
			case DAILY -> this.determineEarliestNextOccurrence(taskStart, from);

			case WEEKLY -> {
				// this.byDay is guaranteed to be never empty
				from = this.determineEarliestNextOccurrence(taskStart, from);

				var nextDayOfWeek = this.getNextDayOfWeek(from);
				yield from.with(nextOrSame(nextDayOfWeek));
			}
			case MONTHLY -> {
				if (this.byDay.isEmpty()) {
					// for now only byDay is implemented for MONTHLY
					yield null;
				}

				// determine the earliest possible next occurrence
				from = this.determineEarliestNextOccurrence(taskStart, from);

				var nextDayOfWeek = this.getNextDayOfWeek(from);
				var nDay = this.getNextDay(nextDayOfWeek);

				int desiredNth = nDay.nthOfPeriod() != null ? nDay.nthOfPeriod() : 1;

				// start with the first day of the current month
				var monthStart = from.withDayOfMonth(1);

				// first occurrence of the weekday in this month
				var firstWeekdayOfMonth = monthStart.with(nextOrSame(nextDayOfWeek));

				// calculate the desired nth occurrence
				var occurrence = firstWeekdayOfMonth.plusWeeks(desiredNth - 1);

				// check if it is still within the same month
				if (occurrence.getMonth() == monthStart.getMonth() && !occurrence.isBefore(from)) {
					yield occurrence;
				}

				// otherwise move to next month
				var nextMonthStart = monthStart.plusMonths(1);
				var firstWeekdayNextMonth = nextMonthStart.with(nextOrSame(nextDayOfWeek));
				yield firstWeekdayNextMonth.plusWeeks(desiredNth - 1);
			}
			case YEARLY -> null; // not implemented
			};

			// Check if result is after the 'until' date
			if (this.until != null && result.toLocalDate().isAfter(this.until)) {
				return null;
			}
			return result;
		}

		private NDay getNextDay(DayOfWeek dow) {
			if (this.byDay.isEmpty()) {
				return null;
			}
			// next day in list
			// if last day, start with first day again
			return this.byDay.stream()//
					.filter(nd -> nd.day.getValue() >= dow.getValue())//
					.findFirst()//
					.orElse(this.byDay.first());
		}

		private DayOfWeek getNextDayOfWeek(ZonedDateTime from) {
			if (this.byDay.isEmpty()) {
				return null;
			}
			var current = from.getDayOfWeek();
			// next weekday in list
			// if last weekday, start with first day again
			return this.byDay.stream()//
					.map(NDay::day)//
					.filter(d -> d.getValue() >= current.getValue()).findFirst()//
					.orElse(this.byDay.first().day());
		}

		private ZonedDateTime determineEarliestNextOccurrence(LocalDateTime taskStart, ZonedDateTime from) {
			// If today's execution time has already passed, start searching from tomorrow
			if (from.toLocalTime().isAfter(taskStart.toLocalTime())) {
				from = from.plusDays(1);
			}

			// Align the time of day to the task start time (keep date, overwrite time)
			return from.with(NANO_OF_DAY, taskStart.toLocalTime().toNanoOfDay());
		}
	}
}
