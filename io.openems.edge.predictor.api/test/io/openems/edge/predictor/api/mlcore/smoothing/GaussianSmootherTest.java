package io.openems.edge.predictor.api.mlcore.smoothing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
