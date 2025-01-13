package io.openems.edge.common.type;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Collections.unmodifiableNavigableMap;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
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
	 * Immutable Map of Quarters (rounded down to 15 minutes) and their respective
	 * predicted values.
	 */
	protected final ImmutableSortedMap<ZonedDateTime, T> valuePerQuarter;

	protected QuarterlyValues(ImmutableSortedMap<ZonedDateTime, T> valuePerQuarter) {
		// Validate times
		if (valuePerQuarter.keySet().stream() //
				.anyMatch(t -> !t.isEqual(roundDownToQuarter(t)))) {
			throw new IllegalArgumentException("Times must be rounded to quarters: " //
					+ valuePerQuarter.keySet().stream() //
							.map(ZonedDateTime::toString) //
							.collect(Collectors.joining(",")));
		}

		this.valuePerQuarter = valuePerQuarter;
	}

	protected QuarterlyValues(ZonedDateTime time, T[] values) {
		time = roundDownToQuarter(time);
		var result = ImmutableSortedMap.<ZonedDateTime, T>naturalOrder();
		for (var value : values) {
			if (value != null) {
				result.put(time, value);
			}
			time = time.plusMinutes(15);
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
	 * Gets the {@link ZonedDateTime} of the first value.
	 * 
	 * @return ZonedDateTime or null if there are no values
	 */
	public final ZonedDateTime getFirstTime() {
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
	 * Gets the {@link ZonedDateTime} of the last value.
	 * 
	 * @return ZonedDateTime or null if there are no values
	 */
	public final ZonedDateTime getLastTime() {
		var lastEntry = this.valuePerQuarter.lastEntry();
		if (lastEntry != null) {
			return lastEntry.getKey();
		}
		return null;
	}

	/**
	 * Gets the value at the given {@link ZonedDateTime}.
	 * 
	 * <p>
	 * As a fallback, this internally also compares via Instant, see
	 * {@link ZonedDateTime#isEqual(java.time.chrono.ChronoZonedDateTime)}.
	 * 
	 * @param time the {@link ZonedDateTime}
	 * @return the value; or null
	 */
	public final T getAt(ZonedDateTime time) {
		return this.getAtOrElse(time, null);
	}

	/**
	 * Gets the value at the given {@link ZonedDateTime}.
	 * 
	 * <p>
	 * As a fallback, this internally also compares via Instant, see
	 * {@link ZonedDateTime#isEqual(java.time.chrono.ChronoZonedDateTime)}.
	 * 
	 * @param time   the {@link ZonedDateTime}
	 * @param orElse the alternative value
	 * @return the value; or the alternative value if none was found
	 */
	public final T getAtOrElse(ZonedDateTime time, T orElse) {
		var result = this.valuePerQuarter.get(time);
		if (result != null) {
			return result;
		}
		// Fallback: compare Instant
		return this.valuePerQuarter.entrySet().stream() //
				.filter(e -> e.getKey().isEqual(time)) //
				.map(Entry::getValue) //
				.findFirst() //
				.orElse(orElse);
	}

	/**
	 * Gets a {@link Stream} of values between from (inclusive) and to (exclusive).
	 * 
	 * @param from the from {@link ZonedDateTime}
	 * @param to   the to {@link ZonedDateTime}
	 * @return a Stream of values; possibly empty
	 */
	public final Stream<T> getBetween(ZonedDateTime from, ZonedDateTime to) {
		return Stream.iterate(from, t -> t.plusMinutes(15)) //
				.takeWhile(t -> t.isBefore(to)) //
				.map(t -> this.getAt(t)) //
				.filter(Objects::nonNull);
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
	public NavigableMap<ZonedDateTime, T> toMapWithAllQuarters() {
		if (this.isEmpty()) {
			return emptyNavigableMap();
		}
		var first = this.getFirstTime();
		var last = this.getLastTime();
		final var result = new TreeMap<ZonedDateTime, T>();
		Stream.iterate(first, t -> t.plusMinutes(15)) //
				.takeWhile(t -> !t.isAfter(last)) //
				.forEach(t -> result.put(t, this.getAt(t)));
		return unmodifiableNavigableMap(result);
	}

	protected T[] asArray(IntFunction<T[]> generator) {
		return this.valuePerQuarter.values().toArray(generator);
	}
}
