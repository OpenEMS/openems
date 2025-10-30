package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.profileclusteringmodel.ColumnNames;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClustererFitter;

public class ProfileClusteringTrainingService {

	private final ClustererFitter clustererFitter;

	public ProfileClusteringTrainingService(ClustererFitter clustererFitter) {
		this.clustererFitter = clustererFitter;
	}

	/**
	 * Clusters the given daily time series data and assigns cluster labels.
	 *
	 * @param timeSeriesByDate the time series data indexed by date
	 * @return a DataFrame containing dates and their corresponding cluster labels
	 */
	public ClusteringResult clusterTimeSeries(DataFrame<LocalDate> timeSeriesByDate) {
		var dates = timeSeriesByDate.getIndex();
		var clusterer = this.clustererFitter.fit(timeSeriesByDate);
		var clusterLabels = clusterer.predict(timeSeriesByDate).stream()//
				.map(Integer::doubleValue)//
				.map(Arrays::asList)//
				.toList();

		var rawClusterLabelMatrix = new DataFrame<>(//
				dates, //
				List.of(ColumnNames.LABEL), //
				clusterLabels);

		return new ClusteringResult(clusterer, rawClusterLabelMatrix);
	}

	public record ClusteringResult(//
			Clusterer clusterer, //
			DataFrame<LocalDate> rawClusterLabelMatrix) {
	}
}
