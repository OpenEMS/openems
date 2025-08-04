package io.openems.edge.predictor.api.mlcore.metrics;

import java.util.List;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public final class Metrics {

	/**
	 * Computes the mean absolute error (MAE) between two Series.
	 *
	 * @param actual   the actual values
	 * @param expected the expected values
	 * @return the MAE
	 * @throws IllegalArgumentException if the indices differ
	 */
	public static double meanAbsoluteError(Series<?> actual, Series<?> expected) {
		if (!actual.getIndex().equals(expected.getIndex())) {
			throw new IllegalArgumentException("Series must have the same index");
		}

		return meanAbsoluteError(actual.getValues(), expected.getValues());
	}

	/**
	 * Computes the mean absolute error (MAE) between two lists of values.
	 *
	 * @param actual   the actual values
	 * @param expected the expected values
	 * @return the MAE, or NaN if no valid pairs exist
	 * @throws IllegalArgumentException if the lists differ in size
	 */
	public static double meanAbsoluteError(List<Double> actual, List<Double> expected) {
		if (actual.size() != expected.size()) {
			throw new IllegalArgumentException("Lists must have the same length");
		}

		double sum = 0.0;
		int count = 0;

		for (int i = 0; i < expected.size(); i++) {
			double actualValue = actual.get(i);
			double expectedValue = expected.get(i);
			if (!Double.isNaN(actualValue) && !Double.isNaN(expectedValue)) {
				sum += Math.abs(actualValue - expectedValue);
				count++;
			}
		}

		return count > 0 ? sum / count : Double.NaN;
	}

	private Metrics() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
