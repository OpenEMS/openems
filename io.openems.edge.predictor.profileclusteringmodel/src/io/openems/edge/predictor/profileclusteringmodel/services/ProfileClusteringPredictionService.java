package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.profileclusteringmodel.ColumnNames;

public class ProfileClusteringPredictionService {

	private final Clusterer clusterer;

	public ProfileClusteringPredictionService(Clusterer clusterer) {
		this.clusterer = clusterer;
	}

	/**
	 * Predicts cluster labels for new daily time series data using an existing
	 * model.
	 *
	 * @param timeSeriesByDate the time series data indexed by date
	 * @return a DataFrame containing dates and their predicted cluster labels
	 */
	public DataFrame<LocalDate> predictClusterLabels(DataFrame<LocalDate> timeSeriesByDate) {
		var dates = timeSeriesByDate.getIndex();
		var clusterLabels = this.clusterer.predict(timeSeriesByDate).stream()//
				.map(Integer::doubleValue)//
				.map(Arrays::asList)//
				.toList();

		var clusteringResult = new DataFrame<>(//
				dates, //
				List.of(ColumnNames.LABEL), //
				clusterLabels);

		return clusteringResult;
	}
}
