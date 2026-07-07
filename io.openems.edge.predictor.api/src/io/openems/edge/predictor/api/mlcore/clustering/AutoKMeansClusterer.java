package io.openems.edge.predictor.api.mlcore.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class AutoKMeansClusterer implements Clusterer {

	private final List<double[]> centroids;
	private final List<double[]> upperQuantileCentroids;

	private AutoKMeansClusterer(List<double[]> centroids, List<double[]> upperQuantileCentroids) {
		validateCentroids(centroids, upperQuantileCentroids);
		this.centroids = List.copyOf(centroids);
		this.upperQuantileCentroids = List.copyOf(upperQuantileCentroids);
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
		var clusters = clusterer.cluster(dataPoints);

		var centroids = clusters.stream()//
				.map(CentroidCluster::getCenter)//
				.map(Clusterable::getPoint)//
				.toList();

		var upperQuantileCentroids = clusters.stream()//
				.map(c -> computeQuantileCenter(c.getPoints(), 0.75))//
				.toList();

		return new AutoKMeansClusterer(centroids, upperQuantileCentroids);
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
	 * @param upperQuantileCentroids the list of quantile centroids (75th percentile)
	 * @return an AutoKMeansClusterer initialized with the specified centroids
	 * @throws IllegalArgumentException if centroids are invalid (e.g. null or
	 *                                  inconsistent dimensions)
	 */
	public static AutoKMeansClusterer from(List<double[]> centroids, List<double[]> upperQuantileCentroids) {
		return new AutoKMeansClusterer(centroids, upperQuantileCentroids);
	}

	@Override
	public List<Integer> predict(DataFrame<?> dataframe) {
		validateDataFrame(dataframe);

		return toDataPoints(dataframe).stream()//
				.map(this::predictSingle)//
				.toList();
	}

	@Override
	public List<double[]> getCentroids() {
		return List.copyOf(this.centroids);
	}

	@Override
	public List<double[]> getUpperQuantileCentroids() {
		return List.copyOf(this.upperQuantileCentroids);
	}

	@VisibleForTesting
	static double[] computeQuantileCenter(List<DataPoint> points, double quantile) {
		int dim = points.getFirst().getPoint().length;
		double[] qCenter = new double[dim];

		IntStream.range(0, dim).forEach(d -> {
			double[] values = points.stream()//
					.mapToDouble(p -> p.getPoint()[d])//
					.toArray();

			var percentile = new Percentile();
			percentile.setData(values);
			qCenter[d] = percentile.evaluate(quantile * 100.0);
		});

		return qCenter;
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

	private static void validateCentroids(List<double[]> centroids, List<double[]> upperQuantileCentroids) {
		if (centroids == null || upperQuantileCentroids == null) {
			throw new IllegalArgumentException("Centroid lists must not be null");
		}
		if (centroids.isEmpty() || upperQuantileCentroids.isEmpty()) {
			throw new IllegalArgumentException("Centroid lists must not be empty");
		}
		if (centroids.size() != upperQuantileCentroids.size()) {
			throw new IllegalArgumentException("Centroid lists must have the same size (clusters)");
		}
		int dim = centroids.getFirst().length;
		Stream.concat(centroids.stream(), upperQuantileCentroids.stream())//
				.filter(c -> c == null || c.length != dim)//
				.findAny()//
				.ifPresent(c -> {
					throw new IllegalArgumentException("Each centroid must be non-null and have the same length");
				});
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
