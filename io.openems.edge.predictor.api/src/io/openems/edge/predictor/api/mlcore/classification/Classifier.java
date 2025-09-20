package io.openems.edge.predictor.api.mlcore.classification;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public interface Classifier {

	/**
	 * Predicts the labels for a set of feature vectors.
	 *
	 * @param features a DataFrame containing the feature vectors for which to
	 *                 predict labels
	 * @return a list of predicted labels (as integers)
	 */
	public List<Integer> predict(DataFrame<?> features);

	/**
	 * Predicts the label for a single feature vector.
	 *
	 * @param features a list of feature values representing a single sample
	 * @return the predicted label as an integer
	 */
	public int predict(List<Double> features);
}