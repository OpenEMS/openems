package io.openems.common.utils;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Collections.unmodifiableNavigableMap;

import java.lang.reflect.Array;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.timedata.DurationUnit;

public class TimeRangeValues<T> {

	private static Instant roundDownToUnit(Instant time, DurationUnit unit) {
		return time.truncatedTo(unit);
	}

	private static Instant roundUpToUnit(Instant time, DurationUnit unit) {
		time = time.truncatedTo(DurationUnit.ofSeconds(1));
		var truncatedTime = time.truncatedTo(unit);
		if (time.isAfter(truncatedTime)) {
			return truncatedTime.plus(unit.getDuration());
		} else {
			return truncatedTime;
		}
	}

	private static int getAmountOfTimeData(Instant fromInclusive, Instant toExclusive, DurationUnit unit) {
		fromInclusive = roundDownToUnit(fromInclusive, unit);
		toExclusive = roundDownToUnit(toExclusive, unit);

		return (int) unit.between(fromInclusive, toExclusive);
	}

	protected static <T> ImmutableSortedMap<Instant, T> convertArrayToDataMap(Instant time, DurationUnit resolution,
			T[] values) {
		time = roundDownToUnit(time, resolution);
		var result = ImmutableSortedMap.<Instant, T>naturalOrder();
		for (var value : values) {
			if (value != null) {
				result.put(time, value);
			}
			time = time.plus(resolution.getDuration());
		}

		return result.build();
	}

	/**
	 * Starts a new builder to construct a new {@link TimeRangeValues} dictionary.
	 * 
	 * @param from       Starting timestamp (inclusive)
	 * @param to         Ending timestamp (Exclusive)
	 * @param unit       Unit in which data is stored
	 * @param valueClazz Value type
	 * @param <T>        Value type
	 * @return New builder
	 */
	public static <T> TimeRangeValuesBuilder<T> builder(Instant from, Instant to, DurationUnit unit,
			Class<T> valueClazz) {
		return new TimeRangeValuesBuilder<T>(from, to, unit, valueClazz);
	}

	/**
	 * Starts a new builder to construct a new {@link TimeRangeValues} dictionary.
	 *
	 * @param timeSpan   Timespan
	 * @param unit       Unit in which data is stored
	 * @param valueClazz Value type
	 * @param <T>        Value type
	 * @return New builder
	 */
	public static <T> TimeRangeValuesBuilder<T> builder(TimeSpan timeSpan, DurationUnit unit, Class<T> valueClazz) {
		return new TimeRangeValuesBuilder<T>(timeSpan.getStartInclusive(), timeSpan.getEndExclusive(), unit,
				valueClazz);
	}

	protected static <T> TimeRangeValues<T> createByValuesArray(Instant time, DurationUnit resolution, T[] values) {
		Objects.requireNonNull(resolution, "Resolution cannot be null");
		Objects.requireNonNull(time, "Time cannot be null");

		var data = convertArrayToDataMap(time, resolution, values);
		return new TimeRangeValues<T>(resolution, data);
	}

	protected static <T> TimeRangeValues<T> createByExisting(TimeRangeValues<T> source, DurationUnit newResolution,
			Function<List<T>, T> valueAggregator) {
		Objects.requireNonNull(newResolution, "Resolution cannot be null");

		if (source.isEmpty()) {
			return new TimeRangeValues<>(newResolution, ImmutableSortedMap.of());
		}

		var lastTime = source.getEndTime();
		var firstTime = source.getFirstTime();

		var reader = new TimeRangeValuesBuilder.DataCloneReader<T>(source.entries());
		var result = ImmutableSortedMap.<Instant, T>naturalOrder();

		var time = roundDownToUnit(firstTime, newResolution);
		while (time.isBefore(lastTime)) {
			var timeSpan = TimeSpan.between(time, time.plus(newResolution.getDuration()));

			var sourceValues = reader.read(timeSpan);
			T aggregatedValue = valueAggregator.apply(sourceValues);
			if (aggregatedValue != null) {
				result.put(time, aggregatedValue);
			}

			time = timeSpan.getEndExclusive();
		}

		return new TimeRangeValues<>(newResolution, result.build());
	}

	/**
	 * Immutable Map of Quarters (rounded down to 15 minutes) and their respective
	 * predicted values.
	 */
	protected final ImmutableSortedMap<Instant, T> valuesByRoundDownTime;

	protected final DurationUnit resolution;

	protected TimeRangeValues(DurationUnit resolution, ImmutableSortedMap<Instant, T> values) {
		Objects.requireNonNull(resolution, "Resolution cannot be null");
		this.resolution = resolution;
		this.valuesByRoundDownTime = values;
	}

	/**
	 * Returns {@code true} if there are no values.
	 *
	 * @return {@code true} if there are no values
	 */
	public final boolean isEmpty() {
		return this.valuesByRoundDownTime.isEmpty();
	}

	/**
	 * Returns {@code true} when future data exists.
	 *
	 * @param clock System clock
	 * @return true if there is future data
	 */
	public boolean hasDataInFuture(Clock clock) {
		var endTime = this.getEndTime();
		if (endTime == null) {
			return false;
		}

		return endTime.isAfter(clock.instant());
	}

	public final DurationUnit getResolution() {
		return this.resolution;
	}

	/**
	 * Gets the {@link Instant} of the first value.
	 *
	 * @return Instant or null if there are no values
	 */
	public final Instant getFirstTime() {
		if (this.valuesByRoundDownTime.isEmpty()) {
			return null;
		}
		return this.valuesByRoundDownTime.firstKey();
	}

	/**
	 * Gets the first value.
	 *
	 * @return the value; or null
	 */
	public final T getFirst() {
		if (this.valuesByRoundDownTime.isEmpty()) {
			return null;
		}
		return this.valuesByRoundDownTime.firstEntry().getValue();
	}

	/**
	 * Gets the {@link Instant} of the last value.
	 *
	 * @return ZonedDateTime or null if there are no values
	 */
	public final Instant getLastTime() {
		var lastEntry = this.valuesByRoundDownTime.lastEntry();
		if (lastEntry != null) {
			return lastEntry.getKey();
		}
		return null;
	}

	/**
	 * Returns the time up to which values are available. Returns the end time of
	 * the last value, not the start time.
	 *
	 * @return {@link Instant}
	 */
	public final Instant getEndTime() {
		var lastTime = this.getLastTime();
		if (lastTime == null) {
			return null;
		}

		return lastTime.plus(this.resolution.getDuration());
	}

	public TimeSpan getTimeSpan() {
		return TimeSpan.between(this.getFirstTime(), this.getEndTime());
	}

	/**
	 * Gets the value at the given {@link Instant} - rounded down to 15 minutes.
	 *
	 * @param time the {@link Instant}
	 * @return the value; or null
	 */
	public final T getAt(Instant time) {
		return this.getAtOrElse(time, null);
	}

	/**
	 * Gets the value at the given {@link ZonedDateTime} - rounded down to 15
	 * minutes.
	 *
	 * @param time the {@link Instant}
	 * @return the value; or null
	 */
	public final T getAt(ZonedDateTime time) {
		return this.getAtOrElse(time.toInstant(), null);
	}

	/**
	 * Gets the value at the given {@link Instant} - rounded down to 15 minutes.
	 *
	 * @param time   the {@link Instant}
	 * @param orElse the alternative value
	 * @return the value; or the alternative value if none was found
	 */
	public final T getAtOrElse(Instant time, T orElse) {
		return this.valuesByRoundDownTime.getOrDefault(this.roundDownToKey(time), orElse);
	}

	/**
	 * Gets the value at the given {@link ZonedDateTime} - rounded down to 15
	 * minutes.
	 *
	 * @param time   the {@link ZonedDateTime}
	 * @param orElse the alternative value
	 * @return the value; or the alternative value if none was found
	 */
	public final T getAtOrElse(ZonedDateTime time, T orElse) {
		return this.getAtOrElse(time.toInstant(), orElse);
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (inclusive).
	 *
	 * @param from the from {@link Instant} (inclusive)
	 * @param to   the to {@link Instant} (inclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<Map.Entry<Instant, T>> getBetweenInclusive(Instant from, Instant to) {
		from = this.roundDownToKey(from);
		to = this.roundDownToKey(to);
		return this.valuesByRoundDownTime.subMap(from, true, to, true).entrySet().stream();
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (inclusive).
	 *
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (inclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<Map.Entry<Instant, T>> getBetweenInclusive(ZonedDateTime from, ZonedDateTime to) {
		return this.getBetweenInclusive(from.toInstant(), to.toInstant());
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (exclusive).
	 *
	 * @param from the from {@link Instant} (inclusive)
	 * @param to   the to {@link Instant} (exclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<Map.Entry<Instant, T>> getBetweenExclusive(Instant from, Instant to) {
		from = this.roundDownToKey(from);
		to = this.roundDownToKey(to);
		return this.valuesByRoundDownTime.subMap(from, true, to, false).entrySet().stream();
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (exclusive).
	 *
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (exclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<Map.Entry<Instant, T>> getBetweenExclusive(ZonedDateTime from, ZonedDateTime to) {
		return this.getBetweenExclusive(from.toInstant(), to.toInstant());
	}

	/**
	 * Converts the internal non-nullable value-map to a map with all quarters as
	 * keys.
	 *
	 * @return a map with possible null values
	 */
	public NavigableMap<Instant, T> toMapWithAllTimes() {
		if (this.isEmpty()) {
			return emptyNavigableMap();
		}
		var time = this.getFirstTime();
		final var last = this.getEndTime();
		final var result = new TreeMap<Instant, T>();
		while (time.isBefore(last)) {
			result.put(time, this.getAt(time));
			time = time.plus(this.resolution.getDuration());
		}
		return unmodifiableNavigableMap(result);
	}

	private Instant roundDownToKey(Instant time) {
		return roundDownToUnit(time, this.resolution);
	}

	@Override
	public String toString() {
		var sh = MoreObjects.toStringHelper(this);
		if (this.isEmpty()) {
			sh.addValue("EMPTY");
		} else {
			sh.add("start", this.getFirstTime().toString());
			sh.add("resolution", this.resolution.toString());
			sh.add("values", this.valuesByRoundDownTime.values().stream() //
					.map(String::valueOf) //
					.collect(Collectors.joining(",")));
		}
		return sh.toString();
	}

	protected List<T> asList(boolean addNullValues) {
		var time = this.getFirstTime();
		if (time == null) {
			return new ArrayList<>();
		}
		var lastTime = this.getLastTime();

		var entries = this.valuesByRoundDownTime.entrySet().iterator();
		var result = new ArrayList<T>();

		Map.Entry<Instant, T> currentEntry = entries.next();
		while (currentEntry != null && DateUtils.isBeforeOrEquals(time, lastTime)) {
			if (currentEntry.getKey().equals(time)) {
				result.add(currentEntry.getValue());
				currentEntry = entries.hasNext() ? entries.next() : null;
			} else if (addNullValues) {
				// Missing timeslot
				result.add(null);
			}

			time = time.plus(this.resolution.getDuration());
		}

		return result;
	}

	/**
	 * Returns the internal non-nullable value-map.
	 *
	 * @return the internal non-nullable value-map
	 */
	public ImmutableSortedMap<Instant, T> toMap() {
		return this.valuesByRoundDownTime;
	}

	/**
	 * Returns all entries as stream.
	 *
	 * @return Stream with entries
	 */
	public Stream<Entry<T>> entryStream() {
		return this.valuesByRoundDownTime.entrySet().stream()
				.map(e -> new Entry<>(TimeSpan.between(e.getKey(), e.getKey().plus(this.resolution.getDuration())),
						e.getValue()));
	}

	/**
	 * Returns all entries as array.
	 * 
	 * @return Array with entries
	 */
	@SuppressWarnings("unchecked")
	public Entry<T>[] entries() {
		return this.entryStream().toArray(Entry[]::new);
	}

	public ImmutableSortedMap<Instant, T> getRawValues() {
		return this.valuesByRoundDownTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TimeRangeValues<?> other)) {
			return false;
		}

		return this.resolution.equals(other.resolution)
				&& this.valuesByRoundDownTime.equals(other.valuesByRoundDownTime);
	}

	/**
	 * Clones {@link TimeRangeValues}, but changes the unit.
	 *
	 * @param newUnit         New unit
	 * @param valueAggregator How the old values should be aggregated for the new
	 *                        value
	 * @return New {@link TimeRangeValues} instance with chosen unit.
	 */
	public TimeRangeValues<T> cloneWithDifferentUnit(DurationUnit newUnit, Function<List<T>, T> valueAggregator) {
		return TimeRangeValues.createByExisting(this, newUnit, valueAggregator);
	}

	/**
	 * Json serializer for {@link TimeRangeValues}.
	 *
	 * @param <T>                 Value type
	 * @param valueArrayGenerator Generator function for a new array instance with
	 *                            given size
	 * @param valueSerializer     Serializer for value
	 * @return Json
	 */
	public static <T> JsonSerializer<TimeRangeValues<T>> serializer(IntFunction<T[]> valueArrayGenerator,
			JsonSerializer<T> valueSerializer) {
		return jsonObjectSerializer(json -> {
			var startTime = json.getInstant("start");
			var resolution = DurationUnit.of(json.getDuration("resolution"));
			var rawValues = json.getJsonArray("values");

			var values = valueArrayGenerator.apply(rawValues.size());
			// I need to deserialize/serialize the array manually, because getArray() does
			// not support null values.
			int valuesIndex = 0;
			for (var rawValue : rawValues) {
				if (!rawValue.isJsonNull()) {
					values[valuesIndex] = valueSerializer.deserialize(rawValue);
				}
				valuesIndex++;
			}

			return TimeRangeValues.createByValuesArray(startTime, resolution, values);
		}, (obj) -> {
			var valuesArray = JsonUtils.buildJsonArray();
			for (var value : obj.asList(true)) {
				if (value == null) {
					valuesArray.add(JsonNull.INSTANCE);
				} else {
					valuesArray.add(valueSerializer.serialize(value));
				}
			}

			return JsonUtils.buildJsonObject() //
					.addProperty("start", Optional.ofNullable(obj.getFirstTime()).orElse(Instant.ofEpochSecond(0))) //
					.addProperty("resolution", obj.getResolution().getDuration().toString()) //
					.add("values", valuesArray.build()) //
					.build();
		});
	}

	public record Entry<T>(//
			TimeSpan timeSpan, //
			T value //
	) {
	}

	public static class TimeRangeValuesBuilder<T> {
		private final DurationUnit resolution;
		private final Instant startTime;
		private final T[] values;

		@SuppressWarnings("unchecked")
		private TimeRangeValuesBuilder(Instant from, Instant to, DurationUnit resolution, Class<T> valueClazz) {
			from = roundDownToUnit(from, resolution);
			to = roundUpToUnit(to, resolution);

			this.resolution = resolution;
			this.startTime = from;
			this.values = (T[]) Array.newInstance(valueClazz, getAmountOfTimeData(from, to, resolution));
		}

		public TimeRangeValuesBuilder<T> setByPosition(int position, T value) {
			if (position < 0 || position >= this.values.length) {
				throw new IndexOutOfBoundsException("Position %d is out of range, only %d positions can be set."
						.formatted(position, this.values.length));
			}

			this.values[position] = value;
			return this;
		}

		public TimeRangeValuesBuilder<T> setByTime(Instant time, T value) {
			var roundedTime = roundDownToUnit(time, this.resolution);
			if (roundedTime.isBefore(this.startTime)) {
				throw new IllegalArgumentException(
						"Time %s is before start time of %s".formatted(time, this.startTime));
			}

			var position = (int) this.resolution.between(this.startTime, roundedTime);
			return this.setByPosition(position, value);
		}

		/**
		 * Fills all missing timeslots with value from previous timeslots.
		 *
		 * @return myself
		 */
		public TimeRangeValuesBuilder<T> fillMissingDataWithPreviousData() {
			T previousValue = null;
			for (int i = 0; i < this.values.length; i++) {
				if (this.values[i] == null) {
					this.values[i] = previousValue;
				} else {
					previousValue = this.values[i];
				}
			}
			return this;
		}

		/**
		 * Fills all misisng timeslots with values from a TimeRangeValues dictionary.
		 *
		 * @param values TimeRangeValues dictionary for receiving missing data
		 * @return myself
		 */
		public TimeRangeValuesBuilder<T> fillMissingDataFromData(TimeRangeValues<T> values) {
			Instant time = this.startTime;
			for (int i = 0; i < this.values.length; i++) {
				if (this.values[i] == null) {
					this.values[i] = values.getAt(time);
				}

				time = time.plus(this.resolution.getDuration());
			}
			return this;
		}

		public TimeRangeValues<T> build() {
			return TimeRangeValues.createByValuesArray(this.startTime, this.resolution, this.values);
		}

		private static class DataCloneReader<T> {
			private final Entry<T>[] values;
			private final List<Entry<T>> valuesFromLastRead = new ArrayList<>();
			private int index;

			public DataCloneReader(Entry<T>[] values) {
				this.values = values;
			}

			public List<T> read(TimeSpan timeSpan) {
				var result = new ArrayList<T>();

				this.checkValuesFromLastRead(result, timeSpan);

				while (this.index < this.values.length && this.values[this.index].timeSpan().overlapsWith(timeSpan)) {
					result.add(this.values[this.index].value());
					this.valuesFromLastRead.add(this.values[this.index]);
					this.index++;
				}

				return result;
			}

			private void checkValuesFromLastRead(ArrayList<T> result, TimeSpan timeSpan) {
				var lastReadIterator = this.valuesFromLastRead.iterator();

				while (lastReadIterator.hasNext()) {
					var entry = lastReadIterator.next();
					if (entry.timeSpan().overlapsWith(timeSpan)) {
						result.add(entry.value());
					} else {
						lastReadIterator.remove();
					}
				}
			}
		}
	}
}
