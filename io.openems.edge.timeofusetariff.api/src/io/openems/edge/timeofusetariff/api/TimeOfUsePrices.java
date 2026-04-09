package io.openems.edge.timeofusetariff.api;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Instant;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;

/**
 * Holds individual Time-of-Use prices - one value per 15 minutes.
 *
 * <p>
 * Values have unit '_meta/Currency'/MWh.
 */
public class TimeOfUsePrices extends TimeRangeValues<Double> {

	private static final DurationUnit QUARTERLY_RESOLUTION = DurationUnit.ofMinutes(15);

	/**
	 * Holds an 'empty' {@link TimeOfUsePrices} object, i.e. `pricePerQuarter` map
	 * is empty.
	 */
	public static final TimeOfUsePrices EMPTY_PRICES = new TimeOfUsePrices(ImmutableSortedMap.of());

	/**
	 * Constructs a {@link TimeOfUsePrices} object.
	 * 
	 * <p>
	 * Trailing `nulls` are cut out.
	 *
	 * @param time   the base time of the prices, rounded down to 15 minutes
	 * @param values the quarterly price values; no nulls
	 * @return a {@link TimeOfUsePrices} object
	 */
	public static TimeOfUsePrices from(Instant time, Double... values) {
		if (values.length == 0) {
			return EMPTY_PRICES;
		}

		var data = convertArrayToDataMap(time, QUARTERLY_RESOLUTION, values);
		return new TimeOfUsePrices(data);
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
	public static TimeOfUsePrices from(ImmutableSortedMap<Instant, Double> map) {
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
	public static TimeOfUsePrices from(Instant time, TimeRangeValues<Double> prices) {
		if (time == null || prices == null || prices.isEmpty()) {
			// prices is EMPTY
			return EMPTY_PRICES;
		}
		final var baseTime = roundDownToQuarter(time);
		if (prices instanceof TimeOfUsePrices && prices.getFirstTime().equals(baseTime)) {
			// prices is still valid
			return (TimeOfUsePrices) prices;
		}

		final var newMap = prices.getRawValues().entrySet().stream() //
				.filter(e -> !baseTime.isAfter(e.getKey())) //
				.collect(toImmutableSortedMap(Instant::compareTo, Map.Entry::getKey, Map.Entry::getValue));

		if (newMap.isEmpty()) {
			return EMPTY_PRICES;
		}
		return new TimeOfUsePrices(newMap);
	}

	private TimeOfUsePrices(ImmutableSortedMap<Instant, Double> pricePerQuarter) {
		super(QUARTERLY_RESOLUTION, pricePerQuarter);
	}

	/**
	 * Gets the prices as an array of {@link Double}s.
	 * 
	 * @return prices array
	 */
	public Double[] asArray() {
		return super.asList(false).toArray(Double[]::new);
	}
}