package io.openems.edge.timeofusetariff.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.SortedMap;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.common.type.QuarterlyValues;

/**
 * Holds individual Time-of-Use prices - one value per 15 minutes.
 *
 * <p>
 * Values have unit '_meta/Currency'/MWh.
 */
public class TimeOfUsePrices extends QuarterlyValues<Double> {

	/**
	 * Holds an 'empty' {@link TimeOfUsePrices} object, i.e. `pricePerQuarter` map
	 * is empty.
	 * 
	 * @return an 'empty' {@link TimeOfUsePrices} object
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
	public static TimeOfUsePrices from(ZonedDateTime time, Double... values) {
		if (values.length == 0) {
			return EMPTY_PRICES;
		}
		return new TimeOfUsePrices(time, values);
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
	public static TimeOfUsePrices from(ImmutableSortedMap<ZonedDateTime, Double> map) {
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
		if (time == null || prices == null || prices.valuePerQuarter.isEmpty()) {
			// prices is EMPTY
			return EMPTY_PRICES;
		}
		time = roundDownToQuarter(time);
		if (prices.valuePerQuarter.firstKey().isEqual(time)) {
			// prices is still valid
			return prices;
		}
		final var newMap = prices.valuePerQuarter.tailMap(time, true);
		if (newMap.isEmpty()) {
			// new prices would be empty
			return EMPTY_PRICES;
		}
		return new TimeOfUsePrices(newMap);
	}

	private TimeOfUsePrices(ImmutableSortedMap<ZonedDateTime, Double> pricePerQuarter) {
		super(pricePerQuarter);
	}

	private TimeOfUsePrices(ZonedDateTime time, Double... values) {
		super(time, values);
	}

	/**
	 * Gets the prices as an array of {@link Double}s.
	 * 
	 * @return prices array
	 */
	public Double[] asArray() {
		return super.asArray(Double[]::new);
	}
}