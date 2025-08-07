package io.openems.edge.predictor.api.mlcore.clustering;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public interface Clusterer {

	/**
	 * Predicts cluster assignments for new data points based on the fitted model.
	 *
	 * @param dataframe a DataFrame containing feature vectors to assign to clusters
	 * @return a list of cluster indices for each row in the DataFrame
	 */
	List<Integer> predict(DataFrame<?> dataframe);

	/**
	 * Returns the centroids of the clusters found during fitting.
	 *
	 * @return a list of centroids, each represented as an array of doubles
	 */
	List<double[]> getCentroids();
}
