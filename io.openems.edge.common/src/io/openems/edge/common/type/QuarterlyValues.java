package io.openems.edge.common.type;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Collections.unmodifiableNavigableMap;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;

public abstract class QuarterlyValues<T> {

	/**
	 * Gets a {@link Stream} of {@link Instant Instants} between from (inclusive)
	 * and to (exclusive).
	 * 
	 * @param from the from {@link Instant} (inclusive)
	 * @param to   the to {@link Instant} (exclusive)
	 * @return a Stream of {@link Instant}s
	 */
	public static Stream<Instant> streamQuartersExclusive(Instant from, Instant to) {
		return Stream.iterate(from, t -> t.plus(15, MINUTES)) //
				.takeWhile(t -> t.isBefore(to));
	}

	/**
	 * Gets a {@link Stream} of {@link ZonedDateTime ZonedDateTimes} between from
	 * (inclusive) and to (exclusive).
	 * 
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (exclusive)
	 * @return a Stream of {@link ZonedDateTime}s
	 */
	public static Stream<ZonedDateTime> streamQuartersExclusive(ZonedDateTime from, ZonedDateTime to) {
		return Stream.iterate(from, t -> t.plus(15, MINUTES)) //
				.takeWhile(t -> t.isBefore(to));
	}

	/**
	 * Gets a {@link Stream} of {@link Instant Instants} between from (inclusive)
	 * and to (inclusive).
	 * 
	 * @param from the from {@link Instant} (inclusive)
	 * @param to   the to {@link Instant} (inclusive)
	 * @return a Stream of {@link Instant}s
	 */
	public static Stream<Instant> streamQuartersInclusive(Instant from, Instant to) {
		return Stream.iterate(from, t -> t.plus(15, MINUTES)) //
				.takeWhile(t -> !t.isAfter(to));
	}

	/**
	 * Gets a {@link ZonedDateTime} of {@link Instant Instants} between from
	 * (inclusive) and to (inclusive).
	 * 
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (inclusive)
	 * @return a Stream of {@link ZonedDateTime}s
	 */
	public static Stream<ZonedDateTime> streamQuartersInclusive(ZonedDateTime from, ZonedDateTime to) {
		return Stream.iterate(from, t -> t.plus(15, MINUTES)) //
				.takeWhile(t -> !t.isAfter(to));
	}

	/**
	 * Immutable Map of Quarters (rounded down to 15 minutes) and their respective
	 * predicted values.
	 */
	protected final ImmutableSortedMap<Instant, T> valuePerQuarter;

	protected QuarterlyValues(ImmutableSortedMap<Instant, T> valuePerQuarter) {
		// Validate times
		if (valuePerQuarter.keySet().stream() //
				.anyMatch(t -> !t.equals(roundDownToQuarter(t)))) {
			throw new IllegalArgumentException("Times must be rounded to quarters: " //
					+ valuePerQuarter.keySet().stream() //
							.map(Instant::toString) //
							.collect(Collectors.joining(",")));
		}

		this.valuePerQuarter = valuePerQuarter;
	}

	protected QuarterlyValues(Instant time, T[] values) {
		time = roundDownToQuarter(time);
		var result = ImmutableSortedMap.<Instant, T>naturalOrder();
		for (var value : values) {
			if (value != null) {
				result.put(time, value);
			}
			time = time.plus(15, MINUTES);
		}
		this.valuePerQuarter = result.build();
	}

	/**
	 * Returns {@code true} if there are no values.
	 *
	 * @return {@code true} if there are no values
	 */
	public final boolean isEmpty() {
		return this.valuePerQuarter.isEmpty();
	}

	/**
	 * Gets the {@link Instant} of the first value.
	 * 
	 * @return Instant or null if there are no values
	 */
	public final Instant getFirstTime() {
		if (this.valuePerQuarter.isEmpty()) {
			return null;
		}
		return this.valuePerQuarter.firstKey();
	}

	/**
	 * Gets the first value.
	 * 
	 * @return the value; or null
	 */
	public final T getFirst() {
		if (this.valuePerQuarter.isEmpty()) {
			return null;
		}
		return this.valuePerQuarter.firstEntry().getValue();
	}

	/**
	 * Gets the {@link Instant} of the last value.
	 * 
	 * @return ZonedDateTime or null if there are no values
	 */
	public final Instant getLastTime() {
		var lastEntry = this.valuePerQuarter.lastEntry();
		if (lastEntry != null) {
			return lastEntry.getKey();
		}
		return null;
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
		return this.valuePerQuarter.getOrDefault(roundDownToQuarter(time), orElse);
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
	public final Stream<T> getBetweenInclusive(Instant from, Instant to) {
		return streamQuartersInclusive(from, to) //
				.map(t -> this.getAt(t)) //
				.filter(Objects::nonNull);
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (inclusive).
	 * 
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (inclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<T> getBetweenInclusive(ZonedDateTime from, ZonedDateTime to) {
		return this.getBetweenInclusive(from.toInstant(), to.toInstant());
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (exclusive).
	 * 
	 * @param from the from {@link Instant} (inclusive)
	 * @param to   the to {@link Instant} (exclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<T> getBetweenExclusive(Instant from, Instant to) {
		return streamQuartersExclusive(from, to) //
				.map(t -> this.getAt(t)) //
				.filter(Objects::nonNull);
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (exclusive).
	 * 
	 * @param from the from {@link ZonedDateTime} (inclusive)
	 * @param to   the to {@link ZonedDateTime} (exclusive)
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<T> getBetweenExclusive(ZonedDateTime from, ZonedDateTime to) {
		return this.getBetweenExclusive(from.toInstant(), to.toInstant());
	}

	@Override
	public String toString() {
		var sh = MoreObjects.toStringHelper(this);
		if (this.isEmpty()) {
			sh.addValue("EMPTY");
		} else {
			sh.add("start", this.getFirstTime().toString());
			sh.add("values", this.toMapWithAllQuarters().values().stream() //
					.map(Object::toString) //
					.collect(Collectors.joining(",")));
		}
		return sh.toString();
	}

	/**
	 * Converts the internal non-nullable value-map to a map with all quarters as
	 * keys.
	 * 
	 * @return a map with possible null values
	 */
	public NavigableMap<Instant, T> toMapWithAllQuarters() {
		if (this.isEmpty()) {
			return emptyNavigableMap();
		}
		var first = this.getFirstTime();
		var last = this.getLastTime();
		final var result = new TreeMap<Instant, T>();
		streamQuartersInclusive(first, last) //
				.forEach(t -> result.put(t, this.getAt(t)));
		return unmodifiableNavigableMap(result);
	}

	protected T[] asArray(IntFunction<T[]> generator) {
		return this.valuePerQuarter.values().toArray(generator);
	}

	/**
	 * Returns the internal non-nullable value-map.
	 * 
	 * @return the internal non-nullable value-map
	 */
	public ImmutableSortedMap<Instant, T> toMap() {
		return this.valuePerQuarter;
	}
}
