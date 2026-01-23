package io.openems.edge.predictor.api.mlcore.metrics;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public final class Metrics {

	/**
	 * Computes the squared error between two {@link Series}.
	 *
	 * @param actual   the actual values
	 * @param expected the expected values
	 * @return the squared error
	 * @throws IllegalArgumentException if the indices differ
	 */
	public static double squaredError(Series<?> actual, Series<?> expected) {
		if (!actual.getIndex().equals(expected.getIndex())) {
			throw new IllegalArgumentException("Series must have the same index");
		}

		return squaredError(actual.getValues(), expected.getValues());
	}

	/**
	 * Computes the squared error between two lists of values.
	 *
	 * @param actual   the actual values
	 * @param expected the expected values
	 * @return the squared error
	 * @throws IllegalArgumentException if one of the lists is {@code null} or
	 *                                  differ in size
	 */
	public static double squaredError(List<Double> actual, List<Double> expected) {
		if (actual == null || expected == null) {
			throw new IllegalArgumentException("Input lists must not be null");
		}

		if (actual.size() != expected.size()) {
			throw new IllegalArgumentException("Lists must have the same length");
		}

		double sum = 0.0;

		for (int i = 0; i < actual.size(); i++) {
			double a = actual.get(i);
			double e = expected.get(i);

			if (Double.isNaN(a) || Double.isNaN(e)) {
				continue;
			}

			double diff = a - e;
			sum += diff * diff;
		}

		return sum;
	}

	private Metrics() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
