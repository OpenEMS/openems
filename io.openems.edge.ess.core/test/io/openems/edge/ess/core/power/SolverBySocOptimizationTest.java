package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.StringJoiner;

import org.junit.Test;

import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.nearequal.SolverBySocOptimization;

public class SolverBySocOptimizationTest {

	private static TargetDirection getDirection(double num) {
		if (num == 0) {
			return TargetDirection.KEEP_ZERO;
		}
		return num < 0 ? TargetDirection.CHARGE : TargetDirection.DISCHARGE;
	}

	@Test
	public void test0() {

		double[] upperBound = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		double[] lowerBound = { -24999.0, -24999.0, -24999.0, -24999.0, -24999.0, -24999.0, -24999.0, -24999.0 };
		double[] socDistribution = { 7, 7, 5, 9, 6, 14, 5, 14 };
		double powerSetValue = -20000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-20000.0, Arrays.stream(solution).sum(), 1e-6);

	}

	@Test
	public void test1() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -500.0, -500.0 };
		double[] socDistribution = { 10.0, 20.0, 30.0, 40.0 };
		double powerSetValue = 10000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(10000.0, Arrays.stream(solution).sum(), 1e-6);

	}

	@Test
	public void test2() {
		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -500.0, -500.0 };
		double[] socDistribution = { 10.0, 20.0, 30.0, 40.0 };
		double powerSetValue = -10000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-10000.0, Arrays.stream(solution).sum(), 1e-6);

	}

	@Test
	public void test3() {
		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -10000.0, -10000.0 };
		double[] socDistribution = { 10.0, 20.0, 30.0, 40.0 };
		double powerSetValue = -10000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-10000.0, Arrays.stream(solution).sum(), 1e-6);

	}

	@Test
	public void test4() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -10000.0, -10000.0 };
		double[] socDistribution = { 25, 25, 25, 25 };
		double powerSetValue = -10000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-10000.0, Arrays.stream(solution).sum(), 1e-6);
	}

	@Test
	public void test5() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -2000.0, -2000.0, -2000.0, 0.0 };
		double[] socDistribution = { 10, 20, 30, 40 };
		double powerSetValue = -10000.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);
		assertEquals(-6000.0, Arrays.stream(solution).sum(), 1e-6);

	}

	@Test
	public void test6() {
		double[] upperBound = { 10000.0, 10000.0, 10000.0, 2000.0 };
		double[] lowerBound = { -2000.0, -2000.0, -2000.0, 0.0 };
		double[] socDistribution = { 10, 20, 30, 40 };
		double powerSetValue = -200.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));

		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-200.0, Arrays.stream(solution).sum(), 1e-6);
	}

	@Test
	public void test7() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 10000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -10000.0, -10000.0 };
		double[] socDistribution = { 10, 20, 30, 40 };
		double powerSetValue = 39995.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(39995.0, Arrays.stream(solution).sum(), 1e-6);
	}

	@Test
	public void test8() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 0.0 };
		double[] lowerBound = { -10000.0, -10000.0, -10000.0, -10000.0 };
		double[] socDistribution = { 10, 20, 30, 40 };
		double powerSetValue = 39995.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(30000.0, Arrays.stream(solution).sum(), 1e-6);
	}

	@Test
	public void test9() {

		double[] upperBound = { 10000.0, 10000.0, 10000.0, 10000.0 };
		double[] lowerBound = { -10000.0, -10000.0, -10000.0, 0.0 };
		double[] socDistribution = { 10, 20, 30, 40 };
		double powerSetValue = -39995.0;

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));
		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-30000.0, Arrays.stream(solution).sum(), 1e-6);
	}

	private static String formatArray(String label, double[] array) {
		StringJoiner joiner = new StringJoiner(", ");
		for (double v : array) {
			joiner.add(String.format("%,.2f", v));
		}
		return label + joiner.toString();
	}

	/**
	 * Helper to print if want to test.
	 * 
	 * @param socDistribution the array of socDistribution
	 * @param upperBound      the array of upperBound
	 * @param solution        the array of solution
	 * @param lowerBound      the array of
	 * @param powerSetValue   the set power
	 * @return result String to print
	 */
	public static String printing(double[] socDistribution, double[] upperBound, double[] solution, double[] lowerBound,
			double powerSetValue) {

		StringBuilder sb = new StringBuilder();
		sb.append("---------------------------\n");
		sb.append(formatArray("SOC Distribution   : ", socDistribution)).append("\n");
		sb.append(formatArray("Upper Bound        : ", upperBound)).append("\n");
		sb.append(formatArray("Solution           : ", solution)).append("\n");

		double totalSolution = Arrays.stream(solution).sum();
		sb.append(String.format("Set power           : %,.2f\n", totalSolution));
		sb.append(String.format("Actual power        : %,.2f\n", powerSetValue));
		sb.append(formatArray("Lower Bound         : ", lowerBound)).append("\n");
		sb.append("---------------------------");

		return sb.toString();
	}

}