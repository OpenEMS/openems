package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	public void solverTest1() {
		double[] upperBound = { 67260, 92000 };
		double[] lowerBound = { -70800, -10590 };
		double[] socDistribution = { 76, 64 };
		double powerSetValue = -40000;
		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue));

		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		assertEquals(-40000.0, Arrays.stream(solution).sum(), 1e-6);
	}

	@Test
	public void testHardwareScenarioBugReplication() {
		// Replicating the hardware scenario from logs:
		// ess1[SoC:98%|Allowed:-28920;68685] ess2[SoC:99%|Allowed:-21180;92000]
		// powerSetValue = 50000 (charge) but hardware shows exceeding limits

		double[] upperBound = { 68685.0, 100605.0 }; // Discharge limits
		double[] lowerBound = { -28920.0, -21180.0 }; // Charge limits
		double[] socDistribution = { 98.0, 99.0 }; // High SOC
		double powerSetValue = -50000.0; // 50kW charge request (absolute value)

		double[] solution = SolverBySocOptimization.solveDistribution(upperBound, lowerBound, socDistribution,
				powerSetValue, getDirection(powerSetValue)); // Direction = CHARGE

		// Print this if test fails
		printing(socDistribution, upperBound, solution, lowerBound, powerSetValue);

		// Assertions
		double totalPower = Arrays.stream(solution).sum();

		// 1. Total power should not exceed available charge capacity
		double maxAvailableChargePower = Math.abs(lowerBound[0]) + Math.abs(lowerBound[1]); // 28920 + 21180 = 50100
		assertTrue(Math.abs(totalPower) <= maxAvailableChargePower,
				"Total power " + Math.abs(totalPower) + " should not exceed available " + maxAvailableChargePower);

		// 2. Individual ESS should not exceed their charge limits
		for (int i = 0; i < solution.length; i++) {
			assertTrue("ESS" + (i + 1) + " should not exceed charge limit",
					solution[i] <= Math.abs(lowerBound[i]) + 1e-6);
		}

		// 3. Power should be distributed roughly proportional to remaining capacity
		// (inverse SOC)
		// At 98-99% SOC, both should get similar but limited charge power

		// Expected: ess1 gets remaining power after ess2 maxed out
		assertEquals("ESS1 should get remaining power", -28920.0, solution[0], 1e-6);
		assertEquals("ESS2 should be at its charge limit", -21080.0, solution[1], 1e-6);
	}

	private static String formatArray(double[] array) {
		StringJoiner joiner = new StringJoiner(", ");
		for (double v : array) {
			joiner.add(String.format("%,.2f", v));
		}
		return joiner.toString();
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

		// 1. SOC Distribution
		sb.append(String.format("%-20s %s%n", "SOC Distribution:", formatArray(socDistribution)));

		// 2. Power set vs calculated
		double totalSolution = Arrays.stream(solution).sum();
		double difference = totalSolution - powerSetValue;

		sb.append(String.format("%-20s %,.2f%n", "Power (Set):", powerSetValue));
		sb.append(String.format("%-20s %,.2f%n", "Power (Calculated):", totalSolution));

		if (Math.abs(difference) < 0.001) {
			sb.append(String.format("%-20s %,.2f %n", "Difference:", difference));
		} else {
			sb.append(String.format("%-20s %,.2f %n", "Difference:", difference));
		}

		sb.append("\n");

		sb.append("Solution range check:\n");
		for (int i = 0; i < solution.length; i++) {
			sb.append(String.format("%,.2f  <  %,.2f  <  %,.2f%n", lowerBound[i], solution[i], upperBound[i]));
		}

		sb.append("\n");

		// 4. Lower and upper bounds (optional for reference)
		sb.append(String.format("%-20s %s%n", "Lower Bound:", formatArray(lowerBound)));
		sb.append(String.format("%-20s %s%n", "Upper Bound:", formatArray(upperBound)));

		sb.append("---------------------------");
		return sb.toString();
	}

}