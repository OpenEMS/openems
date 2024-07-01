package io.openems.edge.timeofusetariff.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static java.util.Collections.emptySortedMap;
import static java.util.Collections.unmodifiableSortedMap;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Holds individual Time-of-Use prices - one value per 15 minutes.
 *
 * <p>
 * Values have unit '_meta/Currency'/MWh.
 */
public class TimeOfUsePrices {

	/**
	 * Holds an 'empty' {@link TimeOfUsePrices} object, i.e. `pricePerQuarter` map
	 * is empty.
	 * 
	 * @return an 'empty' {@link TimeOfUsePrices} object
	 */
	public static final TimeOfUsePrices EMPTY_PRICES = new TimeOfUsePrices(emptySortedMap());

	/**
	 * Constructs a {@link TimeOfUsePrices} object.
	 * 
	 * <p>
	 * Trailing `nulls` are cut out.
	 *
	 * @param time   the base time of the prices, rounded down to 15 minutes
	 * @param values the quarterly price values.
	 * @return a {@link TimeOfUsePrices} object
	 */
	public static TimeOfUsePrices from(ZonedDateTime time, Double... values) {
		return from(buildMap(time, values));
	}

	/**
	 * Constructs a {@link TimeOfUsePrices} object.
	 * 
	 * <p>
	 * Postprocessing is applied:
	 * 
	 * <ul>
	 * <li>Map keys are rounded down to full quarters (15 minutes)
	 * <li>Gaps in keys are filled (value = null)
	 * <li>Trailing null values are removed
	 * </ul>
	 * 
	 * @param map a {@link SortedMap} of times and prices
	 * @return a {@link TimeOfUsePrices} object
	 */
	public static TimeOfUsePrices from(SortedMap<ZonedDateTime, Double> map) {
		map = postprocesMap(map);
		if (map.isEmpty()) {
			return EMPTY_PRICES;
		}
		return new TimeOfUsePrices(map);
	}

	/**
	 * Returns a {@link TimeOfUsePrices} object by deriving data from given
	 * `prices`, but skipping values before given `time`.
	 * 
	 * <p>
	 * If time of given object is still valid, the object is returned. Otherwise a
	 * new object is created.
	 * 
	 * @param time   the base time of the prices, rounded down to 15 minutes
	 * @param prices the source {@link TimeOfUsePrices} object
	 * @return a {@link TimeOfUsePrices} object
	 */
	public static TimeOfUsePrices from(ZonedDateTime time, TimeOfUsePrices prices) {
		if (time == null || prices == null || prices.isEmpty()) {
			// prices is EMPTY
			return EMPTY_PRICES;
		}
		time = roundDownToQuarter(time);
		if (prices.pricePerQuarter.firstKey().isEqual(time)) {
			// prices is still valid
			return prices;
		}
		final var newMap = prices.pricePerQuarter.tailMap(time);
		if (newMap.isEmpty()) {
			// new prices would be empty
			return EMPTY_PRICES;
		}
		return new TimeOfUsePrices(newMap);
	}

	/**
	 * Unmodifiable Map of Quarters (rounded to 15 minutes) and their respective
	 * prices. Values can be null.
	 */
	public final SortedMap<ZonedDateTime, Double> pricePerQuarter;

	private TimeOfUsePrices(SortedMap<ZonedDateTime, Double> pricePerQuarter) {
		// We use unmodifiableSortedMap instead of the more expressive Guava
		// ImmutableSortedMap, because we require `null` values.
		this.pricePerQuarter = unmodifiableSortedMap(pricePerQuarter);
	}

	/**
	 * Returns {@code true} if this map contains no prices.
	 *
	 * @return {@code true} if this map contains no prices
	 */
	public boolean isEmpty() {
		return this.pricePerQuarter.isEmpty();
	}

	/**
	 * Gets the first price in the map; or null if the map is empty.
	 * 
	 * @return price or null
	 */
	public Double getFirst() {
		if (this.pricePerQuarter.isEmpty()) {
			return null;
		}
		return this.pricePerQuarter.get(this.pricePerQuarter.firstKey());
	}

	/**
	 * Gets the prices as an array of {@link Double}s.
	 * 
	 * @return prices array
	 */
	public Double[] asArray() {
		return this.pricePerQuarter.values().toArray(Double[]::new);
	}

	protected static SortedMap<ZonedDateTime, Double> buildMap(ZonedDateTime time, Double... values) {
		time = roundDownToQuarter(time);
		var result = new TreeMap<ZonedDateTime, Double>();
		for (var value : values) {
			result.put(time, value);
			time = time.plusMinutes(15);
		}
		return result;
	}

	protected static SortedMap<ZonedDateTime, Double> postprocesMap(SortedMap<ZonedDateTime, Double> map) {
		var values = new ArrayList<>(map.values());
		var lastNonNullIndex = IntStream.range(0, values.size()) //
				.filter(i -> values.get(i) != null) //
				.reduce((first, second) -> second) //
				.orElse(-1);

		var result = map.entrySet().stream() //
				.limit(lastNonNullIndex + 1) // remove trailing nulls
				.collect(TreeMap<ZonedDateTime, Double>::new,
						(m, e) -> m.put(roundDownToQuarter(e.getKey()), e.getValue()), TreeMap::putAll);

		if (!result.isEmpty()) {
			// Fill gaps
			for (var time = result.firstKey(); time.isBefore(result.lastKey()); time = time.plusMinutes(15)) {
				result.putIfAbsent(time, null);
			}
		}

		return result;
	}
}