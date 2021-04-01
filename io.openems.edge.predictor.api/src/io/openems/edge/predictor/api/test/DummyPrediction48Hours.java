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
public class DummyPrediction48Hours extends Prediction24Hours{


	private final static int DUMMY_NUMBER_OF_VALUES = Prediction24Hours.NUMBER_OF_VALUES * 2;
	private final Integer[] values = new Integer[DUMMY_NUMBER_OF_VALUES];
	
	/**
	 * Holds a {@link DummyPrediction48Hours} with all values null.
	 */
	public final static DummyPrediction48Hours EMPTY = new DummyPrediction48Hours(new Integer[0]);

	/**
	 * Constructs a {@link DummyPrediction48Hours}.
	 * 
	 * @param values the minimum 96 prediction values
	 */
	public DummyPrediction48Hours(Integer... values) {
		super();
		for (int i = 0; i < DUMMY_NUMBER_OF_VALUES && i < values.length; i++) {
			this.values[i] = values[i];
		}
	}

	@Override
	public Integer[] getValues() {
		return this.values;
	}
}
