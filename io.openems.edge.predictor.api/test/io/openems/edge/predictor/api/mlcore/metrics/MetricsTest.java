package io.openems.edge.predictor.api.mlcore.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MetricsTest {

	@Test
	public void testSquaredErrorLists_ShouldReturnCorrectResult() {
		var actual = List.of(1.0, 2.0, 3.0);
		var expected = List.of(1.5, 2.5, 3.5);

		double se = Metrics.squaredError(actual, expected);
		// (1-1.5)^2 + (2-2.5)^2 + (3-3.5)^2 = 0.25 + 0.25 + 0.25 = 0.75
		assertEquals(0.75, se, 1e-9);
	}

	@Test
	public void testSquaredErrorLists_ShouldReturnCorrectResult_WhenNaNValues() {
		var actual = List.of(1.0, Double.NaN, 3.0);
		var expected = List.of(1.5, 2.5, Double.NaN);

		double se = Metrics.squaredError(actual, expected);
		// (1-1.5)^2 = 0.25
		assertEquals(0.25, se, 1e-9);
	}

	@Test
	public void testSquaredErrorLists_ShouldReturnZero_WhenEmptyInput() {
		List<Double> actual = List.of();
		List<Double> expected = List.of();

		double se = Metrics.squaredError(actual, expected);
		assertEquals(0.0, se, 1e-9);
	}

	@Test
	public void testSquaredErrorLists_ShouldThrow_WhenDifferentInputLengths() {
		var actual = List.of(1.0);
		var expected = List.of(1.0, 2.0);

		assertThrows(IllegalArgumentException.class, () -> {
			Metrics.squaredError(actual, expected);
		});
	}

	@Test
	public void testSquaredErrorLists_ShouldThrow_WhenNullInput() {
		List<Double> actual = null;
		List<Double> expected = List.of(1.0);

		assertThrows(IllegalArgumentException.class, () -> {
			Metrics.squaredError(actual, expected);
		});
	}

	@Test
	public void testSquaredErrorSeries_ShouldReturnCorrectResult() {
		var actual = toSeries(List.of(1.0, 2.0, 3.0));
		var expected = toSeries(List.of(1.5, 2.5, 3.5));

		double se = Metrics.squaredError(actual, expected);
		assertEquals(0.75, se, 1e-9);
	}

	@Test
	public void testSquaredErrorSeries_ShouldThrow_WhenDifferentIndex() {
		var actual = toSeries(List.of(0.0, 1.0, 3.0));
		var expected = new Series<>(List.of(4, 5, 6), List.of(1.0, 2.0, 3.0));

		assertThrows(IllegalArgumentException.class, () -> {
			Metrics.squaredError(actual, expected);
		});
	}

	private static Series<Integer> toSeries(List<Double> values) {
		var index = IntStream.range(0, values.size())//
				.boxed()//
				.toList();
		return new Series<>(index, values);
	}
}
