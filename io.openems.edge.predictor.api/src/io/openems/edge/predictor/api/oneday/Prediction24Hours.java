package io.openems.edge.predictor.api.oneday;

import java.util.Arrays;

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

	private final Integer[] values = new Integer[NUMBER_OF_VALUES];

	/**
	 * Holds a {@link Prediction24Hours} with all values null.
	 */
	public static final Prediction24Hours EMPTY = new Prediction24Hours();

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
		return new Prediction24Hours(sumValues);
	}

	/**
	 * Constructs a {@link Prediction24Hours}.
	 *
	 * @param values the 96 prediction values
	 */
	public Prediction24Hours(Integer... values) {
		for (var i = 0; i < NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
	}

	public Integer[] getValues() {
		return this.values;
	}

	@Override
	public String toString() {
		return "Prediction24Hours " + Arrays.toString(this.values);
	}

}
