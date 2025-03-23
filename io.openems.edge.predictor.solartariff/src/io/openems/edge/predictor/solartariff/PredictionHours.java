package io.openems.edge.predictor.solartariff;

import io.openems.edge.common.type.TypeUtils;

/**
 * Holds a prediction for next hours; one value per hour; defaults to 24 values in total.
 * 
 * <p>
 * Values have the same unit as the base Channel, i.e. if the Prediction relates
 * to _sum/ProductionGridActivePower, the value is in unit Watt and represents
 * the average Watt within a period.
 */
public class PredictionHours {

	public final static int NUMBER_OF_VALUES = 24;

	private final Integer[] values = new Integer[NUMBER_OF_VALUES];

	/**
	 * Holds a {@link PredictionHours} with all values null.
	 */
	public final static PredictionHours EMPTY = new PredictionHours(new Integer[0]);

	/**
	 * Sums up the given {@link PredictionHours}s. If any source value is null,
	 * the result value is also null.
	 * 
	 * @param predictions the given {@link PredictionHours}
	 * @return a {@link PredictionHours} holding the sum of all predictions.
	 */
	public static PredictionHours sum(PredictionHours... predictions) {
		final Integer[] sumValues = new Integer[NUMBER_OF_VALUES];
		for (int i = 0; i < NUMBER_OF_VALUES; i++) {
			Integer sumValue = null;
			for (PredictionHours prediction : predictions) {
				if (prediction.values[i] == null) {
					sumValue = null;
					break;
				} else {
					sumValue = TypeUtils.sum(sumValue, prediction.values[i]);
				}
			}
			sumValues[i] = sumValue;
		}
		return new PredictionHours(sumValues);
	}

	/**
	 * Constructs a {@link PredictionHours}.
	 * 
	 * @param values then next prediction values
	 */
	public PredictionHours(Integer[] values) {
		super();
		for (int i = 0; i < NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
	}

	public Integer[] getValues() {
		return this.values;
	}

}