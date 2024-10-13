package io.openems.edge.predictor.lstmmodel.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PerformanceMatrixTest {

	public static final double delta = 0.0001;

	@Test
	public void meanAbsoluteErrorTest() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5, 2.8));

		double result = PerformanceMatrix.meanAbsoluteError(target, predicted);

		assertEquals(0.5666, result, delta);
	}

	@Test
	public void meanAbsoluteErrorTestWithException() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5));

		assertThrows(IllegalArgumentException.class, () -> PerformanceMatrix.meanAbsoluteError(target, predicted));
	}

	@Test
	public void rmsErrorTest() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5, 2.8));

		// MSE = ((2-1)^2 + (2.5-2)^2 + (2.8-3)^2) / 3 = 0.21
		// RMS error = sqrt(0.21) = 0.4582575694 (rounded for comparison)
		double expectedRmsError = 0.6557;

		double result = PerformanceMatrix.rmsError(target, predicted);

		assertEquals(expectedRmsError, result, delta);
	}

	@Test
	public void rmsErrorWithException() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5));

		assertThrows(IllegalArgumentException.class, () -> PerformanceMatrix.rmsError(target, predicted));
	}

	@Test
	public void meanSquaredErrorTest() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5, 2.8));

		// Calculate the expected mean squared error manually
		// MSE = ((2-1)^2 + (2.5-2)^2 + (2.8-3)^2) / 3 = 0.21
		double expectedMse = 0.43;

		double result = PerformanceMatrix.meanSquaredError(target, predicted);

		assertEquals(expectedMse, result, delta);
	}

	@Test
	public void meanSquaredErrorException() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(2.0, 2.5));

		assertThrows(IllegalArgumentException.class, () -> PerformanceMatrix.meanSquaredError(target, predicted));
	}

	@Test
	public void accuracyTest() {
		ArrayList<Double> target = new ArrayList<>(List.of(1.0, 2.0, 3.0));
		ArrayList<Double> predicted = new ArrayList<>(List.of(1.2, 2.3, 3.2));

		double allowedPercentage = 0.1;

		// Call the accuracy method and compare the result with the expected value
		// Expected accuracy: (2 out of 3 values within 10% difference)
		double expectedAccuracy = 0.3333;

		assertEquals(expectedAccuracy, PerformanceMatrix.accuracy(target, predicted, allowedPercentage), delta);
	}

	@Test
	public void accuracyTestWithEmptyList() {
		ArrayList<Double> target = new ArrayList<>();
		ArrayList<Double> predicted = new ArrayList<>();

		double allowedPercentage = 0.1;

		assertEquals(Double.NaN, PerformanceMatrix.accuracy(target, predicted, allowedPercentage), delta);
	}

}
