package io.openems.edge.ess.core.power.solver.nearequal;

import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

public class SolveNearEqual {

	private double[] upperBound;
	private double[] lowerBound;
	private double powerSetValue;

	/**
	 * Solves an optimization problem using the Nelder-Mead simplex algorithm.
	 * 
	 * @param totalVariables The total number of variables in the optimization
	 *                       problem.
	 * @return An array containing the optimal solution for the optimization
	 *         problem.
	 */
	public PointValuePair solve(int totalVariables) {

		// If the SetPower is greater than Sum of inidividual upper bound, then return
		// the upper bound
		if (this.powerSetValue > Arrays.stream(this.upperBound).sum()) {
			return new PointValuePair(this.upperBound, 0.0);
		}

		MultivariateFunctionPenaltyAdapter adapter = new ConstraintWrapper(this.objectiveFunction(), //
				this.lowerBound, // expected lower limit of the result
				this.upperBound, // expected upper limit of the result
				0.0, // offset value
				this.getScaleVal(1, totalVariables), //
				this.powerSetValue);

		OptimizationData[] optimizationData = { new ObjectiveFunction(adapter), //
				this.setInitialValue(0, totalVariables), //
				new NelderMeadSimplex(totalVariables), //
				new MaxIter(10000), //
				new MaxEval(10000), //
				GoalType.MINIMIZE };
		SimplexOptimizer solver = new SimplexOptimizer(1e-11, 1e-11);
		return solver.optimize(optimizationData);
	}

	/**
	 * Creates an objective function that calculates the standard deviation of a
	 * given set of points.
	 * 
	 * @return The objective function that computes the standard deviation.
	 */
	private MultivariateFunction objectiveFunction() {
		MultivariateFunction objective = new MultivariateFunction() {

			@Override
			public double value(double[] point) {
				double mean = Arrays.stream(point).sum() / point.length;
				// Standard deviation
				return Math.sqrt(//
						Arrays.stream((Arrays.stream(point)//
								.map(i -> Math.pow(i - mean, 2))//
								.toArray()))//
								.sum() / point.length);
			}
		};
		return objective;
	}

	/**
	 * Sets up the initial guess for the optimization problem.
	 * 
	 * @param value          The initial value to set for each variable.
	 * @param totalVariables The total number of variables in the optimization
	 *                       problem.
	 * @return An InitialGuess object representing the initial guess for the
	 *         optimization.
	 */

	private InitialGuess setInitialValue(double value, int totalVariables) {
		double[] intialValArray = new double[totalVariables];
		Arrays.fill(intialValArray, value);
		return new InitialGuess(intialValArray);
	}

	/**
	 * Generates an array of scale values with the specified scale value for each
	 * variable.
	 * 
	 * @param scaleVal       The value to set for each scale element.
	 * @param totalVariables The total number of variables for which scale values
	 *                       are generated.
	 * @return An array containing the specified scale value for each variable.
	 */
	private double[] getScaleVal(double scaleVal, int totalVariables) {
		double[] scaleValArray = new double[totalVariables];
		Arrays.fill(scaleValArray, scaleVal);
		return scaleValArray;
	}

	public void setUpperBound(double[] val) {
		this.upperBound = val;
	}

	public void setLowerBound(double[] val) {
		this.lowerBound = val;
	}

	public void setpowerSetValue(double val) {
		this.powerSetValue = val;
	}
}
