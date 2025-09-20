package io.openems.edge.ess.core.power.solver.nearequal;

import java.util.Arrays;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.core.power.data.TargetDirection;

public record SolverBySocOptimization(double[] upperBound, double[] lowerBound, double[] socs, double powerSetValue,
		TargetDirection targetDirection) {

	private static final double DEFAULT_VALUE = 0.0;

	public SolverBySocOptimization {
		if (upperBound == null || lowerBound == null || socs == null || targetDirection == null) {
			throw new IllegalStateException("All fields must be initialized");
		}
	}

	/**
	 * Solve the Power distribution based on the Soc weights.
	 * 
	 * @param totalVariables the total number of variables
	 * @return result the {@link PointValuePair}
	 */
	public PointValuePair solve(int totalVariables) {
		// Handle discharge case
		if (this.targetDirection == TargetDirection.DISCHARGE && this.exceedsTotalUpperBound()) {
			return new PointValuePair(this.upperBound, DEFAULT_VALUE);
		}

		// Handle charge case
		if (this.targetDirection == TargetDirection.CHARGE && this.exceedsTotalLowerBound()) {
			return new PointValuePair(Arrays.stream(this.lowerBound).map(Math::abs).toArray(), DEFAULT_VALUE);
		}

		double[] result = solveDistribution(//
				this.upperBound, //
				this.lowerBound, //
				this.socs, //
				this.powerSetValue, //
				this.targetDirection);

		return new PointValuePair(result, totalVariables);
	}

	private boolean exceedsTotalUpperBound() {
		return this.powerSetValue >= Arrays.stream(this.upperBound).sum();
	}

	private boolean exceedsTotalLowerBound() {
		return this.powerSetValue >= Arrays.stream(this.lowerBound).map(Math::abs).sum();
	}

	/**
	 * Solve distribution.
	 * 
	 * @param upperBound      the upperbound array
	 * @param lowerBound      the lowerBound array
	 * @param socDistribution the socDistribution array
	 * @param powerSetValue   the set Active power variable
	 * @param targetDirection the {@link TargetDirection}
	 * @return result the double array of the solved variables
	 */
	public static double[] solveDistribution(double[] upperBound, double[] lowerBound, double[] socDistribution,
			double powerSetValue, TargetDirection targetDirection) {
		final var n = upperBound.length;

		if (targetDirection == TargetDirection.KEEP_ZERO) {
			return new double[n];
		}

		var isCharging = targetDirection == TargetDirection.CHARGE;

		// Step 1: Calculate the weights
		var weights = isCharging //
				? calculateInverseSocDistribution(socDistribution) //
				: normalizeSocPercentages(socDistribution);

		var weightSum = Arrays.stream(weights).sum();

		if (weightSum == 0) {
			throw new IllegalArgumentException("Weight sum is zero, check input values.");
		}

		// Normalize weights
		weights = Arrays.stream(weights).map(w -> w / weightSum).toArray();

		// Step 2: Set up the linear system Ax = b
		// Mathematical formulation:
		// 1. We want x₁, x₂, ..., xₙ to be //
		// proportional to weights: x₁/w₁ = x₂/w₂ = ... = xₙ/wₙ = k
		// 2. This means x₁ = w₁·k, x₂ = w₂·k, ..., xₙ = wₙ·k
		// 3. We also know x₁ + x₂ + ... + xₙ = powerSetValue
		// 4. Therefore: w₁·k + w₂·k + ... + wₙ·k = powerSetValue
		// 5. Which gives us: k = powerSetValue / (w₁ + w₂ + ... + wₙ)
		//
		// In matrix form, we express this as:
		// 1. Proportionality equations: w₂·x₁ - w₁·x₂ = 0, w₃·x₂ - w₂·x₃ = 0, etc.
		// 2. Sum equation: x₁ + x₂ + ... + xₙ = powerSetValue

		// Define matrix A and vector b for the linear system
		RealMatrix coefficientMatrix = MatrixUtils.createRealMatrix(n, n);
		RealVector constraintVector = new ArrayRealVector(n);

		// First n-1 rows enforce proportionality between adjacent pairs
		for (int i = 0; i < n - 1; i++) {
			coefficientMatrix.setEntry(i, i, weights[i + 1]);
			coefficientMatrix.setEntry(i, i + 1, -weights[i]);
			constraintVector.setEntry(i, 0);
		}

		// Last row enforces the sum constraint
		for (int i = 0; i < n; i++) {
			coefficientMatrix.setEntry(n - 1, i, 1);
		}
		constraintVector.setEntry(n - 1, powerSetValue);

		// Step 3: Solve the system
		DecompositionSolver solver = new LUDecomposition(coefficientMatrix).getSolver();
		RealVector solution = solver.solve(constraintVector);
		double[] result = solution.toArray();

		// Step 4: Iteratively check for bound violations and adjust until no more
		// violations
		boolean boundViolation;
		do {
			boundViolation = false;

			// Check and fix bound violations
			for (int i = 0; i < n; i++) {
				if (isCharging) {
					if (result[i] < lowerBound[i]) {
						result[i] = lowerBound[i];
						boundViolation = true;
					}
				} else {
					if (result[i] > upperBound[i]) {
						result[i] = upperBound[i];
						boundViolation = true;
					}
				}
			}

			// If bounds were violated, solve a reduced system
			if (boundViolation) {
				// Count fixed variables
				int fixedCount = 0;
				for (int i = 0; i < n; i++) {
					if (result[i] == lowerBound[i] || result[i] == upperBound[i]) {
						fixedCount++;
					}
				}

				// Create reduced system for remaining variables
				int freeCount = n - fixedCount;
				if (freeCount > 0) {
					RealMatrix reducedA = MatrixUtils.createRealMatrix(freeCount, freeCount);
					RealVector reducedB = new ArrayRealVector(freeCount);

					// Track free variables
					int[] freeIndices = new int[freeCount];
					int freeIdx = 0;
					for (int i = 0; i < n; i++) {
						if (result[i] != lowerBound[i] && result[i] != upperBound[i]) {
							freeIndices[freeIdx++] = i;
						}
					}

					// force proportionality among free variables
					for (int i = 0; i < freeCount - 1; i++) {
						reducedA.setEntry(i, i, weights[freeIndices[i + 1]]);
						reducedA.setEntry(i, i + 1, -weights[freeIndices[i]]);
						reducedB.setEntry(i, 0);
					}

					// force adjusted sum constraint
					double remainingSum = powerSetValue;
					for (int i = 0; i < n; i++) {
						if (result[i] == lowerBound[i] || result[i] == upperBound[i]) {
							remainingSum -= result[i];
						}
					}

					// Set coefficients for sum constraint
					for (int i = 0; i < freeCount; i++) {
						reducedA.setEntry(freeCount - 1, i, 1);
					}
					reducedB.setEntry(freeCount - 1, remainingSum);

					try {
						// Solve reduced system
						DecompositionSolver reducedSolver = new LUDecomposition(reducedA).getSolver();
						RealVector reducedSolution = reducedSolver.solve(reducedB);

						// Update result
						freeIdx = 0;
						for (int i = 0; i < n; i++) {
							if (result[i] != lowerBound[i] && result[i] != upperBound[i]) {
								result[i] = reducedSolution.getEntry(freeIdx++);
							}
						}
					} catch (SingularMatrixException e) {
						// No unique solution - stop iterating
						boundViolation = false;
					}
				} else {
					// All variables fixed - stop iterating
					boundViolation = false;
				}
			}
		} while (boundViolation);

		return result;
	}

	/**
	 * Normalizes the State of Charge (SOC) percentages by converting them from a
	 * scale of 0-100 to a normalized scale of 0-1.
	 *
	 * @param socs An array of SOC percentages, where each value is in the range [0,
	 *             100].
	 * @return An array of normalized SOC values, where each value is in the range
	 *         [0, 1].
	 */
	private static double[] normalizeSocPercentages(double[] socs) {
		return Arrays.stream(socs).map(p -> p / 100.0).toArray();

	}

	/**
	 * Calculates the inverse distribution of the State of Charge (SOC) percentages.
	 * This represents the non-SOC distribution, which is the complement of the SOC
	 * distribution. For example, if the SOC is 80%, the non-SOC value will be 20%
	 * (or 0.2 in normalized form).
	 *
	 * @param socs An array of SOC percentages, where each value is in the range [0,
	 *             100].
	 * @return An array of non-SOC values, where each value is in the range [0, 1].
	 */
	private static double[] calculateInverseSocDistribution(double[] socs) {
		return Arrays.stream(socs).map(p -> 1 - (p / 100.0)).toArray();
	}

	/**
	 * Builder class for constructing instances of {@link SolverBySocOptimization}.
	 */
	public static class Builder {
		private double[] upperBound;
		private double[] lowerBound;
		private double[] socs;
		private double powerSetValue;
		private TargetDirection targetDirection;

		/**
		 * Sets the upper bound values.
		 *
		 * @param upperBound an array of upper bound values
		 * @return the current {@code Builder}
		 */
		public Builder withUpperBound(double[] upperBound) {
			this.upperBound = upperBound;
			return this;
		}

		/**
		 * Sets the lower bound values.
		 *
		 * @param lowerBound an array of lower bound values
		 * @return the current {@code Builder}
		 */
		public Builder withLowerBound(double[] lowerBound) {
			this.lowerBound = lowerBound;
			return this;
		}

		/**
		 * Sets the state of charge (SoC) values.
		 *
		 * @param socs an array of SoC values
		 * @return the current {@code Builder}
		 */
		public Builder withSocs(double[] socs) {
			this.socs = socs;
			return this;
		}

		/**
		 * Sets the power set value .
		 *
		 * @param powerSetValue the power set value
		 * @return the current {@code Builder}
		 */
		public Builder withPowerSetValue(double powerSetValue) {
			this.powerSetValue = powerSetValue;
			return this;
		}

		/**
		 * Sets the target direction.
		 *
		 * @param targetDirection the {@link TargetDirection} to be set
		 * @return the current {@code Builder}
		 */
		public Builder withTargetDirection(TargetDirection targetDirection) {
			this.targetDirection = targetDirection;
			return this;
		}

		public SolverBySocOptimization build() {
			return new SolverBySocOptimization(this.upperBound, this.lowerBound, this.socs, this.powerSetValue,
					this.targetDirection);
		}
	}

}