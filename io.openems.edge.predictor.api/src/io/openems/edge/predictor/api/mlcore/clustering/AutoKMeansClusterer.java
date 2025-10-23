package io.openems.edge.predictor.api.mlcore.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class AutoKMeansClusterer implements Clusterer {

	private final List<double[]> centroids;

	private AutoKMeansClusterer(List<double[]> centroids) {
		validateCentroids(centroids);
		this.centroids = List.copyOf(centroids);
	}

	/**
	 * Fits a K-Means model to the given DataFrame using the elbow method to
	 * determine the optimal number of clusters.
	 *
	 * @param dataframe the input data
	 * @param minK      minimum number of clusters (must be > 0)
	 * @param maxK      maximum number of clusters (must be ≥ minK)
	 * @return an AutoKMeansClusterer with the computed centroids
	 * @throws IllegalArgumentException if the k range is invalid
	 */
	public static AutoKMeansClusterer fit(DataFrame<?> dataframe, int minK, int maxK) {
		if (minK <= 0 || maxK < minK) {
			throw new IllegalArgumentException("Invalid k range: minK=" + minK + ", maxK=" + maxK);
		}
		validateDataFrame(dataframe);

		var dataPoints = toDataPoints(dataframe);
		int optimalK = findOptimalKUsingElbow(dataPoints, minK, maxK);

		var clusterer = new KMeansPlusPlusClusterer<DataPoint>(optimalK);
		var centroids = clusterer.cluster(dataPoints).stream()//
				.map(CentroidCluster::getCenter)//
				.map(Clusterable::getPoint)//
				.toList();

		return new AutoKMeansClusterer(centroids);
	}

	/**
	 * Creates an AutoKMeansClusterer instance with predefined centroids.
	 * 
	 * <p>
	 * This allows bypassing the automatic selection of cluster count (k) and
	 * directly using the given centroids as cluster centers.
	 *
	 * @param centroids the list of centroid vectors to initialize the clusterer
	 *                  with
	 * @return an AutoKMeansClusterer initialized with the specified centroids
	 * @throws IllegalArgumentException if centroids are invalid (e.g. null or
	 *                                  inconsistent dimensions)
	 */
	public static AutoKMeansClusterer from(List<double[]> centroids) {
		return new AutoKMeansClusterer(centroids);
	}

	@Override
	public List<Integer> predict(DataFrame<?> dataframe) {
		validateDataFrame(dataframe);

		return toDataPoints(dataframe).stream()//
				.map(this::predictSingle)//
				.toList();
	}

	public List<double[]> getCentroids() {
		return List.copyOf(this.centroids);
	}

	private int predictSingle(DataPoint point) {
		int bestCluster = -1;
		double minDistance = Double.POSITIVE_INFINITY;

		for (int i = 0; i < this.centroids.size(); i++) {
			double distance = KMeansUtils.computeSquaredDistance(point.getPoint(), this.centroids.get(i));
			if (distance < minDistance) {
				minDistance = distance;
				bestCluster = i;
			}
		}

		return bestCluster;
	}

	private static int findOptimalKUsingElbow(List<DataPoint> dataPoints, int minK, int maxK) {
		var inertias = new ArrayList<Double>();

		for (int k = minK; k <= maxK; k++) {
			var clusterer = new KMeansPlusPlusClusterer<DataPoint>(k);
			var clusters = clusterer.cluster(dataPoints);
			inertias.add(KMeansUtils.computeInertia(clusters));
		}

		return KMeansUtils.detectKnee(inertias, minK);
	}

	private static void validateCentroids(List<double[]> centroids) {
		if (centroids == null || centroids.isEmpty()) {
			throw new IllegalArgumentException("Centroid list must not be null or empty");
		}
		int length = centroids.get(0).length;
		for (double[] c : centroids) {
			if (c == null || c.length != length) {
				throw new IllegalArgumentException("Each centroid must be non-null and have the same length");
			}
		}
	}

	private static void validateDataFrame(DataFrame<?> dataframe) {
		if (dataframe == null || dataframe.rowCount() == 0) {
			throw new IllegalArgumentException("DataFrame must not be null or empty");
		}
		if (dataframe.columnCount() == 0) {
			throw new IllegalArgumentException("DataFrame must have at least one column");
		}
		for (var row : dataframe.getValues()) {
			for (var value : row) {
				if (value == null || value.isNaN()) {
					throw new IllegalArgumentException("DataFrame contains null or NaN values");
				}
			}
		}
	}

	private static List<DataPoint> toDataPoints(DataFrame<?> dataframe) {
		return dataframe.getValues().stream()//
				.map(list -> list.stream()//
						.mapToDouble(Double::doubleValue)//
						.toArray())//
				.map(DataPoint::new)//
				.toList();
	}
}
