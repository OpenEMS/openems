package io.openems.edge.predictor.api.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.QuarterlyValues;

/**
 * Holds a prediction - one value per 15 minutes.
 *
 * <p>
 * Values have the same unit as the base Channel, i.e. if the Prediction relates
 * to _sum/ProductionGridActivePower, the value is in unit Watt and represents
 * the average Watt within a 15 minutes period.
 */
public class Prediction extends QuarterlyValues<Integer> {

	/**
	 * Holds an 'empty' {@link Prediction} object, i.e. `valuePerQuarter` map is
	 * empty.
	 * 
	 * @return an 'empty' {@link TimeOfUsePrices} object
	 */
	public static final Prediction EMPTY_PREDICTION = new Prediction(ImmutableSortedMap.of());

	/**
	 * Sums up the given {@link Prediction}s. If any source value is null, the
	 * result value is null/removed.
	 *
	 * @param predictions the given {@link Prediction}
	 * @return a {@link Prediction} holding the sum of all predictions.
	 */
	public static Prediction sum(Prediction... predictions) {
		// Evaluate the minimum common time of all source predictions
		final var minTime = Stream.of(predictions) //
				.map(p -> p.valuePerQuarter) //
				.filter(m -> !m.isEmpty()) //
				.map(m -> m.firstKey()) //
				.max(ZonedDateTime::compareTo);
		if (minTime.isEmpty()) {
			return EMPTY_PREDICTION;
		}

		// Evaluate the maximium common of all source predictions
		final var maxTime = Stream.of(predictions) //
				.filter(m -> !m.isEmpty()) //
				.map(p -> p.valuePerQuarter.lastKey()) //
				.min(ZonedDateTime::compareTo);
		if (maxTime.isEmpty()) {
			return EMPTY_PREDICTION;
		}

		final var result = ImmutableSortedMap.<ZonedDateTime, Integer>naturalOrder();
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
		return Prediction.from(result.build());
	}

	private static record ValueRange(Integer min, Integer max) {

		public Integer fitWithin(Integer value) {
			if (this.max != null && value > this.max) {
				value = this.max;
			}
			if (this.min != null && value < this.min) {
				value = this.min;
			}
			return value;
		}
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
		return new Prediction(time, values);
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
	 * Map keys must be full quarters (15 minutes)
	 * 
	 * @param map a {@link SortedMap} of times and prices
	 * @return a {@link Prediction} object
	 */
	public static Prediction from(ImmutableSortedMap<ZonedDateTime, Integer> map) {
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

	private static Prediction from(ValueRange valueRange, ZonedDateTime time, Integer... values) {
		if (values.length == 0) {
			return EMPTY_PREDICTION;
		}
		return new Prediction(time, Stream.of(values) //
				.map(valueRange::fitWithin) // apply ValueRange
				.toArray(Integer[]::new));
	}

	private Prediction(ImmutableSortedMap<ZonedDateTime, Integer> valuePerQuarter) {
		super(valuePerQuarter);
	}

	private Prediction(ZonedDateTime time, Integer... values) {
		super(time, values);
	}

	/**
	 * Gets the prediction as an array of {@link Integer}s.
	 * 
	 * @return prices array
	 */
	public Integer[] asArray() {
		return super.asArray(Integer[]::new);
	}
}
