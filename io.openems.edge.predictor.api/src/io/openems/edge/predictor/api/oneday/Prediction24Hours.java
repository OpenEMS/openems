package io.openems.edge.predictor.api.oneday;

import static io.openems.edge.common.type.TypeUtils.max;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.type.TypeUtils;

/**
 * Holds a prediction for 24 h; one value per 15 minutes; 96 values in total.
 *
 * <p>
 * Values have the same unit as the base Channel, i.e. if the Prediction relates
 * to _sum/ProductionGridActivePower, the value is in unit Watt and represents
 * the average Watt within a 15 minutes period.
 */
public class Prediction24Hours {

	public static final int NUMBER_OF_VALUES = 288;// set to 96; changed for test.

	/**
	 * Holds a {@link Prediction24Hours} with all values null.
	 */
	public static final Prediction24Hours EMPTY = Prediction24Hours.of();

	private final Integer[] values;

	/**
	 * Constructs a {@link Prediction24Hours}.
	 *
	 * @param values the 96 prediction values
	 */
	public Prediction24Hours(Integer... values) {
		this.values = new Integer[NUMBER_OF_VALUES];
		for (var i = 0; i < NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
	}

	/**
	 * Sums up the given {@link Prediction24Hours}s. If any source value is null,
	 * the result value is also null.
	 *
	 * @param predictions the given {@link Prediction24Hours}
	 * @return a {@link Prediction24Hours} holding the sum of all predictions.
	 */
	public static Prediction24Hours sum(Prediction24Hours... predictions) {
		final var sumValues = new Integer[NUMBER_OF_VALUES];
		for (var i = 0; i < NUMBER_OF_VALUES; i++) {
			Integer sumValue = null;
			for (Prediction24Hours prediction : predictions) {
				if (prediction.values[i] == null) {
					sumValue = null;
					break;
				}
				sumValue = TypeUtils.sum(sumValue, prediction.values[i]);
			}
			sumValues[i] = sumValue;
		}
		return Prediction24Hours.of(sumValues);
	}

	public static enum Converter {
		/** No conversion operation. */
		NONE(Function.identity()),
		/** Convert negative values to zero. */
		TO_POSITIVE(v -> max(v, 0));

		/** Conversion function. Input value is guaranteed to be not null. */
		public final Function<Integer, Integer> function;

		private Converter(Function<Integer, Integer> function) {
			this.function = function;
		}
	}

	/**
	 * Gets the default {@link Converter} for the given {@link ChannelAddress}.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @return a {@link Converter}
	 */
	public static Converter converterForChannelAddress(ChannelAddress channelAddress) {
		return switch (channelAddress.toString()) {
		case //
				"_sum/ProductionActivePower", //
				"_sum/ConsumptionActivePower", //
				"_sum/UnmanagedConsumptionActivePower" ->
			Converter.TO_POSITIVE;

		default -> Converter.NONE;
		};
	}

	/**
	 * Constructs a {@link Prediction24Hours} with no {@link Converter}.
	 *
	 * @param values the prediction values; up to 96
	 * @return new {@link Prediction24Hours}
	 */
	public static Prediction24Hours of(Integer... values) {
		return of(Converter.NONE, values);
	}

	/**
	 * Constructs a {@link Prediction24Hours} with the default {@link Converter} for
	 * the given {@link ChannelAddress}.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @param values         the prediction values; up to 96
	 * @return new {@link Prediction24Hours}
	 */
	public static Prediction24Hours of(ChannelAddress channelAddress, Integer... values) {
		return new Prediction24Hours(converterForChannelAddress(channelAddress), values);
	}

	/**
	 * Constructs a {@link Prediction24Hours}.
	 *
	 * @param converter the {@link Converter}
	 * @param values    the prediction values; up to 96
	 * @return new {@link Prediction24Hours}
	 */
	public static Prediction24Hours of(Converter converter, Integer... values) {
		return new Prediction24Hours(converter, values);
	}

	private Prediction24Hours(Converter converter, Integer[] values) {
		this.values = IntStream.range(0, NUMBER_OF_VALUES) //
				.mapToObj(i -> i < values.length ? values[i] : null) //
				// Apply converter
				.map(v -> v == null //
						? null //
						: converter.function.apply(v)) //
				.toArray(Integer[]::new);
	}

	public Integer[] getValues() {
		return this.values;
	}

	@Override
	public String toString() {
		return "Prediction24Hours " + Arrays.toString(this.values);
	}

}
