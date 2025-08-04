package io.openems.edge.predictor.api.mlcore.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class MetricsTest {

	@Test
	public void testMeanAbsoluteErrorLists_ShouldReturnCorrectResult() {
		var actual = List.of(1.0, 2.0, 3.0);
		var expected = List.of(1.5, 2.5, 3.5);

		double mae = Metrics.meanAbsoluteError(actual, expected);
		assertEquals(0.5, mae, 1e-9);
	}

	@Test
	public void testMeanAbsoluteErrorLists_ShouldReturnCorrectResult_WhenNaNValues() {
		var actual = List.of(1.0, Double.NaN, 3.0);
		var expected = List.of(1.5, 2.5, Double.NaN);

		double mae = Metrics.meanAbsoluteError(actual, expected);
		assertEquals(0.5, mae, 1e-9);
	}

	@Test
	public void testMeanAbsoluteErrorLists_ShouldReturnNaN_WhenEmptyInput() {
		List<Double> actual = List.of();
		List<Double> expected = List.of();

		double mae = Metrics.meanAbsoluteError(actual, expected);
		assertTrue(Double.isNaN(mae));
	}

	@Test
	public void testMeanAbsoluteErrorLists_ShouldThrow_WhenDifferentInputLengths() {
		var actual = List.of(1.0);
		var expected = List.of(1.0, 2.0);

		assertThrows(//
				IllegalArgumentException.class, //
				() -> Metrics.meanAbsoluteError(actual, expected));
	}

	@Test
	public void testMeanAbsoluteErrorSeries_ShouldReturnCorrectResult() {
		var actual = toSeries(List.of(1.0, 2.0, 3.0));
		var expected = toSeries(List.of(1.5, 2.5, 3.5));

		double mae = Metrics.meanAbsoluteError(actual, expected);
		assertEquals(0.5, mae, 1e-9);
	}

	@Test
	public void testMeanAbsoluteErrorSeries_ShouldThrow_WhenDifferentIndex() {
		var actual = toSeries(List.of(0.0, 1.0, 3.0));
		var expected = new Series<>(List.of(4, 5, 6), List.of(1.0, 2.0, 3.0));

		assertThrows(//
				IllegalArgumentException.class, //
				() -> Metrics.meanAbsoluteError(actual, expected));
	}

	private static Series<Integer> toSeries(List<Double> values) {
		var index = IntStream.range(0, values.size())//
				.boxed()//
				.toList();
		return new Series<>(index, values);
	}
}
