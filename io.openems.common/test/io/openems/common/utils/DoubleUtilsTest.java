package io.openems.common.utils;

import static io.openems.common.utils.DoubleUtils.normalize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.OptionalDouble;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DoubleUtilsTest {

	@Test
	public void testGetOrNull() {
		assertNull(DoubleUtils.getOrNull(OptionalDouble.empty()));
		assertEquals(5., DoubleUtils.getOrNull(OptionalDouble.of(5.)), 0.001);
	}

	@Test
	public void testDoubleToDoubleFunction() {
		assertEquals(10., new DoubleUtils.DoubleToDoubleFunction() {

			@Override
			public double apply(double value) {
				return value * 2;
			}
		}.apply(5.), 0.001);
	}

	@Nested
	@DisplayName("normalize()")
	public class NormalizeTest {

		@Test
		public void shouldReturnMinNormalized_whenValueBelowMin() {
			final double result = normalize(50, 100, 1000, 0, 1, false);

			assertEquals(0, result);
		}

		@Test
		public void shouldReturnMaxNormalized_whenValueAboveMax() {
			final double result = normalize(1500, 100, 1000, 0, 1, false);

			assertEquals(1, result);
		}

		@Test
		public void shouldNormalizeCorrectly_whenValueWithinRange() {
			final double result = normalize(550, 100, 1000, 0, 1, false);

			assertEquals(0.5, result, 1e-9);
		}

		@Test
		public void shouldReturnMinNormalized_whenValueEqualsMin() {
			final double result = normalize(100, 100, 1000, 0, 1, false);

			assertEquals(0, result);
		}

		@Test
		public void shouldReturnMaxNormalized_whenValueEqualsMax() {
			final double result = normalize(1000, 100, 1000, 0, 1, false);

			assertEquals(1, result);
		}

		@Test
		public void shouldInvertResult_whenInvertIsTrue() {
			final double result = normalize(100, 100, 1000, 0, 1, true);

			assertEquals(1, result);
		}

		@Test
		public void shouldInvertMiddleValueCorrectly() {
			final double result = normalize(550, 100, 1000, 0, 1, true);

			assertEquals(0.5, result, 1e-9);
		}

		@Test
		public void shouldReturnMinNormalized_whenMinEqualsMax_andNotInverted() {
			final double result = normalize(500, 100, 100, 0, 1, false);

			assertEquals(0, result);
		}

		@Test
		public void shouldReturnMaxNormalized_whenMinEqualsMax_andInverted() {
			final double result = normalize(500, 100, 100, 0, 1, true);

			assertEquals(1, result);
		}

		@Test
		public void shouldReturnMinNormalized_whenMinNotFinite() {
			final double result = normalize(500, Double.NaN, 1000, 0, 1, false);

			assertEquals(0, result);
		}

		@Test
		public void shouldReturnMinNormalized_whenMaxNotFinite() {
			final double result = normalize(500, 100, Double.POSITIVE_INFINITY, 0, 1, false);

			assertEquals(0, result);
		}

		@Test
		public void shouldReturnMaxNormalized_whenNotFinite_andInverted() {
			final double result = normalize(500, Double.NaN, 1000, 0, 1, true);

			assertEquals(1, result);
		}

		@Test
		public void shouldWorkWithCustomNormalizedRange() {
			final double result = normalize(550, 100, 1000, 10, 20, false);

			assertEquals(15, result, 1e-9);
		}

		@Test
		public void shouldInvertWithCustomNormalizedRange() {
			final double result = normalize(550, 100, 1000, 10, 20, true);

			assertEquals(15, result, 1e-9);
		}
	}
}
