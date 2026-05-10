package io.openems.edge.predictor.api.mlcore.regression;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public interface Regressor {

	/**
	 * Predicts target values for the given feature matrix.
	 *
	 * @param features the input features
	 * @return a list of predicted values
	 */
	public List<Double> predict(DataFrame<?> features);
}
