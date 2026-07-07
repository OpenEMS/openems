package io.openems.edge.predictor.api.mlcore.clustering;

import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;

public final class KMeansUtils {

	/**
	 * Computes the squared Euclidean distance between two vectors.
	 *
	 * @param a the first vector
	 * @param b the second vector
	 * @return the squared distance
	 * @throws IllegalArgumentException if the vectors have different lengths
	 */
	public static double computeSquaredDistance(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("Vectors must be of the same length");
		}
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			double d = a[i] - b[i];
			sum += d * d;
		}
		return sum;
	}

	/**
	 * Computes the total within-cluster sum of squared distances (inertia) for a
	 * list of clusters.
	 *
	 * @param clusters the list of clusters
	 * @return the total inertia
	 */
	public static double computeInertia(List<CentroidCluster<DataPoint>> clusters) {
		double sum = 0;
		for (var cluster : clusters) {
			double[] center = cluster.getCenter().getPoint();
			for (var point : cluster.getPoints()) {
				sum += computeSquaredDistance(point.getPoint(), center);
			}
		}
		return sum;
	}

	/**
	 * Detects the optimal number of clusters (the "knee") using the elbow method on
	 * a list of inertia values.
	 *
	 * @param inertias the list of inertia values for increasing k
	 * @param minK     the minimum k corresponding to the first inertia value
	 * @return the detected optimal k
	 * @throws IllegalArgumentException if input is invalid
	 */
	public static int detectKnee(List<Double> inertias, int minK) {
		if (inertias == null || inertias.isEmpty()) {
			throw new IllegalArgumentException("Inertia list must not be null or empty");
		}
		if (minK < 0) {
			throw new IllegalArgumentException("minK must be >= 0");
		}

		int n = inertias.size();
		double x1 = minK;
		double y1 = inertias.get(0);
		double x2 = minK + n - 1;
		double y2 = inertias.get(n - 1);

		double maxDistance = -1;
		int bestK = minK;

		for (int i = 0; i < n; i++) {
			double x0 = minK + i;
			double y0 = inertias.get(i);
			double dist = pointToLineDistance(x0, y0, x1, y1, x2, y2);
			if (dist > maxDistance) {
				maxDistance = dist;
				bestK = (int) x0;
			}
		}

		return bestK;
	}

	private static double pointToLineDistance(double x0, double y0, double x1, double y1, double x2, double y2) {
		double numerator = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);
		double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
		return numerator / denominator;
	}

	private KMeansUtils() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
