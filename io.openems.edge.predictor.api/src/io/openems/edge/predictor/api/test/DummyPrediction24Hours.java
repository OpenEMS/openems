package io.openems.edge.predictor.api.test;

import io.openems.edge.predictor.api.oneday.Prediction24Hours;

/**
 * Holds a prediction for 24 h; one value per 15 minutes; 96 values in total.
 *
 * <p>
 * Values have the same unit as the base Channel, i.e. if the Prediction relates
 * to _sum/ProductionGridActivePower, the value is in unit Watt and represents
 * the average Watt within a 15 minutes period.
 */
public class DummyPrediction24Hours extends Prediction24Hours {

	private static final int DUMMY_NUMBER_OF_VALUES = Prediction24Hours.NUMBER_OF_VALUES * 2;
	private final Integer[] values = new Integer[DUMMY_NUMBER_OF_VALUES];

	/**
	 * Holds a {@link DummyPrediction24Hours} with all values null.
	 */
	public static final DummyPrediction24Hours EMPTY = new DummyPrediction24Hours();

	/**
	 * Constructs a {@link DummyPrediction24Hours}.
	 *
	 * @param values the minimum 96 prediction values
	 */
	public DummyPrediction24Hours(Integer... values) {
		for (var i = 0; i < DUMMY_NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
	}

	@Override
	public Integer[] getValues() {
		return this.values;
	}
}
