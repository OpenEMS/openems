package io.openems.edge.predictor.api.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.common.type.TypeUtils.max;
import static io.openems.edge.common.type.TypeUtils.min;
import static java.util.Collections.emptySortedMap;
import static java.util.Collections.unmodifiableSortedMap;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;

/**
 * Holds a prediction - one value per 15 minutes.
 *
 * <p>
 * Values have the same unit as the base Channel, i.e. if the Prediction relates
 * to _sum/ProductionGridActivePower, the value is in unit Watt and represents
 * the average Watt within a 15 minutes period.
 */
public class Prediction {

	/**
	 * Holds an 'empty' {@link Prediction} object, i.e. `valuePerQuarter` map is
	 * empty.
	 * 
	 * @return an 'empty' {@link TimeOfUsePrices} object
	 */
	public static final Prediction EMPTY_PREDICTION = new Prediction(emptySortedMap());

	/**
	 * Sums up the given {@link Prediction}s. If any source value is null, the
	 * result value is also null.
	 *
	 * @param predictions the given {@link Prediction}
	 * @return a {@link Prediction} holding the sum of all predictions.
	 */
	public static Prediction sum(Prediction... predictions) {
		final var minTime = Stream.of(predictions) //
				.map(p -> p.valuePerQuarter) //
				.filter(m -> !m.isEmpty()) //
				.map(m -> m.firstKey()) //
				.max(ZonedDateTime::compareTo);
		if (minTime.isEmpty()) {
			return EMPTY_PREDICTION;
		}
		final var maxTime = Stream.of(predictions) //
				.map(p -> p.valuePerQuarter.lastKey()) //
				.min(ZonedDateTime::compareTo);
		if (maxTime.isEmpty()) {
			return EMPTY_PREDICTION;
		}
		final var result = new TreeMap<ZonedDateTime, Integer>();
		for (var time = minTime.get(); !time.isAfter(maxTime.get()); time.plusMinutes(15)) {
			var values = Stream.of(predictions) //
					.map(p -> p.valuePerQuarter.get(time)) //
					.toList();
			final Integer sum;
			if (values.stream().anyMatch(Objects::isNull)) {
				sum = null;
			} else {
				sum = values.stream().mapToInt(Integer::valueOf).sum();
			}
			result.put(time, sum);
		}
		return Prediction.from(UNLIMITED, result);
	}

	private static record ValueRange(Integer min, Integer max) {
	}

	private static final ValueRange UNLIMITED = new ValueRange(null, null);

	/**
	 * Gets the {@link ValueRange} for the given {@link ChannelAddress}.
	 * 
	 * @param sum            the {@link Sum}
	 * @param channelAddress the {@link ChannelAddress}
	 * @return a {@link ValueRange}
	 */
	public static ValueRange getValueRange(Sum sum, ChannelAddress channelAddress) {
		return switch (channelAddress.toString()) {
		case "_sum/ProductionActivePower" //
			-> new ValueRange(0, sum.getProductionMaxActivePower().get());
		case "_sum/ConsumptionActivePower", "_sum/UnmanagedConsumptionActivePower" //
			-> new ValueRange(0, sum.getConsumptionMaxActivePower().get());
		default //
			-> UNLIMITED;
		};
	}

	/**
	 * Constructs a {@link Prediction} object with {@value #UNLIMITED}
	 * {@link ValueRange}.
	 * 
	 * <p>
	 * Trailing `nulls` are cut out.
	 *
	 * @param time   the base time of the prediction values, rounded down to 15
	 *               minutes
	 * @param values the quarterly prediction values.
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(ZonedDateTime time, Integer... values) {
		return from(UNLIMITED, time, values);
	}

	/**
	 * Constructs a {@link Prediction} object with the default {@link ValueRange}
	 * for the given {@link ChannelAddress}.
	 * 
	 * <p>
	 * Trailing `nulls` are cut out.
	 *
	 * @param sum            the {@link Sum}
	 * @param channelAddress the {@link ChannelAddress}
	 * @param time           the base time of the prediction values, rounded down to
	 *                       15 minutes
	 * @param values         the quarterly prediction values.
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(Sum sum, ChannelAddress channelAddress, ZonedDateTime time, Integer... values) {
		return from(getValueRange(sum, channelAddress), time, values);
	}

	/**
	 * Constructs a {@link Prediction} object.
	 * 
	 * <p>
	 * Trailing `nulls` are cut out.
	 *
	 * @param valueRange the {@link ValueRange}
	 * @param time       the base time of the prediction values, rounded down to 15
	 *                   minutes
	 * @param values     the quarterly prediction values.
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(ValueRange valueRange, ZonedDateTime time, Integer... values) {
		return from(valueRange, buildMap(time, values));
	}

	/**
	 * Constructs a {@link Prediction} object.
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
	 * @param valueRange the {@link ValueRange}
	 * @param map        a {@link SortedMap} of times and prices
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(ValueRange valueRange, SortedMap<ZonedDateTime, Integer> map) {
		map = postprocessMap(valueRange, map);
		if (map.isEmpty()) {
			return EMPTY_PREDICTION;
		}
		return new Prediction(map);
	}

	/**
	 * Returns a {@link Prediction} object by deriving data from given `prediction`,
	 * but skipping values before given `time`.
	 * 
	 * <p>
	 * If time of given object is still valid, the object is returned. Otherwise a
	 * new object is created.
	 * 
	 * @param time       the base time of the prices, rounded down to 15 minutes
	 * @param prediction the source {@link Prediction} object
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(ZonedDateTime time, Prediction prediction) {
		if (time == null || prediction == null || prediction.isEmpty()) {
			// prices is EMPTY
			return EMPTY_PREDICTION;
		}
		time = roundDownToQuarter(time);
		if (prediction.valuePerQuarter.firstKey().isEqual(time)) {
			// prices is still valid
			return prediction;
		}
		final var newMap = prediction.valuePerQuarter.tailMap(time);
		if (newMap.isEmpty()) {
			// new prices would be empty
			return EMPTY_PREDICTION;
		}
		return new Prediction(newMap);
	}

	/**
	 * Unmodifiable Map of Quarters (rounded to 15 minutes) and their respective
	 * predicted values. Values can be null.
	 */
	public final SortedMap<ZonedDateTime, Integer> valuePerQuarter;

	private Prediction(SortedMap<ZonedDateTime, Integer> valuePerQuarter) {
		// We use unmodifiableSortedMap instead of the more expressive Guava
		// ImmutableSortedMap, because we require `null` values.
		this.valuePerQuarter = unmodifiableSortedMap(valuePerQuarter);
	}

	/**
	 * Returns {@code true} if this map contains no predictions.
	 *
	 * @return {@code true} if this map contains no predictions
	 */
	public boolean isEmpty() {
		return this.valuePerQuarter.isEmpty();
	}

	/**
	 * Gets the prediction values as an array of {@link Integer}s.
	 * 
	 * @return values array
	 */
	public Integer[] asArray() {
		return this.valuePerQuarter.values().toArray(Integer[]::new);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("Prediction=");
		if (this.isEmpty()) {
			b.append("EMPTY");
		} else {
			b.append(this.valuePerQuarter.firstKey().toString()); //
			b.append("=");
			this.valuePerQuarter.values().forEach(v -> b.append(v).append(","));
		}
		return b.toString();
	}

	protected static SortedMap<ZonedDateTime, Integer> buildMap(ZonedDateTime time, Integer... values) {
		time = roundDownToQuarter(time);
		var result = new TreeMap<ZonedDateTime, Integer>();
		for (var value : values) {
			result.put(time, value);
			time = time.plusMinutes(15);
		}
		return result;
	}

	protected static SortedMap<ZonedDateTime, Integer> postprocessMap(ValueRange valueRange,
			SortedMap<ZonedDateTime, Integer> map) {
		var values = new ArrayList<>(map.values());
		var lastNonNullIndex = IntStream.range(0, values.size()) //
				.filter(i -> values.get(i) != null) //
				.reduce((first, second) -> second) //
				.orElse(-1);

		var result = map.entrySet().stream() //
				.limit(lastNonNullIndex + 1) // remove trailing nulls
				.collect(TreeMap<ZonedDateTime, Integer>::new, //
						(m, e) -> m.put(//
								roundDownToQuarter(e.getKey()), //
								e.getValue() == null ? null : min(valueRange.max, max(valueRange.min, e.getValue()))), //
						TreeMap::putAll);

		if (!result.isEmpty()) {
			// Fill gaps
			for (var time = result.firstKey(); time.isBefore(result.lastKey()); time = time.plusMinutes(15)) {
				result.putIfAbsent(time, null);
			}
		}

		return result;
	}
}
