package io.openems.edge.predictor.api.mlcore.smoothing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GaussianSmootherTest {

	@Test
	void testConstructor_shouldThrowException_whenNullKernel() {
		assertThrows(IllegalArgumentException.class, () -> new GaussianSmoother(null));
	}

	@Test
	void testConstructor_shouldThrowException_whenEmptyKernel() {
		assertThrows(IllegalArgumentException.class, () -> new GaussianSmoother(new double[0]));
	}

	@Test
	void testConstructor_shouldThrowException_whenZeroSumKernel() {
		assertThrows(IllegalArgumentException.class, () -> new GaussianSmoother(new double[] { 1.0, -1.0 }));
	}

	@Test
	void testSmooth_shouldThrowException_whenNullInput() {
		final var smoother = new GaussianSmoother(new double[] { 1.0 });

		assertThrows(IllegalArgumentException.class, () -> smoother.smooth(null));
	}

	@Test
	void testSmooth_shouldThrowException_whenEmptyInput() {
		final var smoother = new GaussianSmoother(new double[] { 1.0 });

		assertThrows(IllegalArgumentException.class, () -> smoother.smooth(new double[0]));
	}

	@Test
	void testSmooth_shouldReturnSameValues_whenIdentityKernel() {
		final var smoother = new GaussianSmoother(new double[] { 1.0 });
		double[] input = { 1.0, 2.0, 3.0, 4.0 };

		double[] result = smoother.smooth(input);

		assertArrayEquals(input, result, 1e-10);
	}

	@Test
	void testSmooth_shouldUseNormalizedKernel() {
		final var smoother = new GaussianSmoother(new double[] { 2.0, 2.0, 2.0 });

		double[] input = { 0.0, 3.0, 0.0 };
		double[] result = smoother.smooth(input);

		double originalSum = Arrays.stream(input).sum();
		double resultSum = Arrays.stream(result).sum();
		assertEquals(originalSum, resultSum, 1e-10);
	}

	@Test
	void testSmooth_shouldPreserveSum() {
		final var smoother = new GaussianSmoother(new double[] { 1.0, 2.0, 1.0 });
		double[] input = { 1.0, 2.0, 3.0, 4.0, 5.0 };

		double[] result = smoother.smooth(input);

		double originalSum = Arrays.stream(input).sum();
		double smoothedSum = Arrays.stream(result).sum();
		assertEquals(originalSum, smoothedSum, 1e-10);
	}

	@Test
	void testSmooth_shouldRemainUniform_whenUniformInput() {
		final var smoother = new GaussianSmoother(new double[] { 1.0, 2.0, 1.0 });
		double[] input = { 5.0, 5.0, 5.0, 5.0, 5.0 };

		double[] result = smoother.smooth(input);

		for (double value : result) {
			assertEquals(5.0, value, 1e-10);
		}
	}

	@Test
	void testSmooth_shouldUseEdgeReflection() {
		final var smoother = new GaussianSmoother(new double[] { 1.0, 1.0, 1.0 });
		double[] input = { 3.0, 0.0, 0.0 };

		double[] result = smoother.smooth(input);

		assertEquals(2.0, result[0], 1e-10);
		assertEquals(1.0, result[1], 1e-10);
		assertEquals(0.0, result[2], 1e-10);
	}

	@Test
	void testSmooth_shouldHandleSingleElementInput() {
		final var smoother = new GaussianSmoother(new double[] { 1.0, 2.0, 1.0 });

		double[] result = smoother.smooth(new double[] { 42.0 });

		assertArrayEquals(new double[] { 42.0 }, result, 1e-10);
	}

	@Test
	void testSmooth_shouldReturnRawSmoothedValues_whenSmoothedSumZero() {
		final var smoother = new GaussianSmoother(new double[] { 1.0 });
		double[] input = { 0.0, 0.0, 0.0 };

		double[] result = smoother.smooth(input);

		assertArrayEquals(input, result, 1e-10);
	}

	@Test
	void testSmooth_outputLengthShouldEqualsInputLength() {
		final var smoother = new GaussianSmoother(new double[] { 1.0, 4.0, 6.0, 4.0, 1.0 });
		double[] input = { 1, 2, 3, 4, 5, 6, 7 };

		double[] result = smoother.smooth(input);

		assertEquals(input.length, result.length);
	}

	@ParameterizedTest
	@CsvSource({ //
			"0, 3, 2.25, 3.375, 3.375", //
			"9, 3, 4.090909090909091, 2.4545454545454546, 2.4545454545454546", //
			"9, 0, 0, 0, 0" //
	})
	void testSmooth_shouldPreserveForecastSumWithHistory(double historicValue, double forecastValue, double firstValue,
			double secondValue, double thirdValue) {
		var smoother = new GaussianSmoother(new double[] { 1, 1, 1 });
		double[] values = { forecastValue, forecastValue, forecastValue };
		double[] history = { historicValue };

		var result = smoother.smooth(values, history);

		assertArrayEquals(new double[] { firstValue, secondValue, thirdValue }, result, 1e-10);
		assertEquals(Arrays.stream(values).sum(), Arrays.stream(result).sum(), 1e-10);
		assertArrayEquals(new double[] { historicValue }, history);
		assertArrayEquals(new double[] { forecastValue, forecastValue, forecastValue }, values);
	}

	@Test
	void testSmooth_shouldExtendShortHistoryAndKeepChronologicalOrder() {
		var smoother = new GaussianSmoother(GaussianKernels.SIZE_9);
		double[] values = { 100, 100, 100, 100, 100, 100 };

		var result = smoother.smooth(values, new double[] { 20, 40 });

		double factor = 600.0 / 544;
		assertArrayEquals(
				new double[] { 71.2 * factor, 83.2 * factor, 92 * factor, 97.6 * factor, 100 * factor, 100 * factor },
				result, 1e-10);
	}

	@Test
	void testSmooth_shouldKeepExistingBehaviorWithEmptyHistory() {
		var smoother = new GaussianSmoother(GaussianKernels.SIZE_9);
		double[] values = { 10, 50, 30, 0, 0 };

		assertArrayEquals(smoother.smooth(values), smoother.smooth(values, new double[0]), 1e-10);
	}

	@Test
	void testSmooth_shouldRejectNullHistory() {
		var smoother = new GaussianSmoother(GaussianKernels.SIZE_9);

		assertThrows(IllegalArgumentException.class, () -> smoother.smooth(new double[] { 1 }, null));
	}
}
