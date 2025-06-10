package io.openems.edge.ess.core.power.solver.nearequal;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionPenaltyAdapter;

public class ConstraintWrapper extends MultivariateFunctionPenaltyAdapter {

	private double[] lower;
	private double[] upper;
	private double[] point;
	private MultivariateFunction function;
	private double[] scale;
	private double setVal;
	private int itteration;
	private double refrence;

	public ConstraintWrapper(MultivariateFunction bounded, double[] lower, double[] upper, double offset,
			double[] scale, double setVal) {

		super(bounded, lower, upper, offset, scale);

		this.lower = lower;
		this.function = bounded;
		this.scale = scale;
		this.upper = upper;
		this.setVal = setVal;
		this.refrence = 0;
		this.itteration = 0;
	}

	/**
	 * Computes the value of the objective function for a given point in the
	 * parameter space.
	 * 
	 * @param point The point in the parameter space for which to evaluate the
	 *              objective function.
	 * @return The computed value of the objective function.
	 */
	@Override
	public double value(double[] point) {
		this.point = point;
		var standerDev = this.function.value(this.point);
		final double convergence = standerDev - this.refrence;
		this.refrence = standerDev;
		var sumoffset = this.sumConstraint(point);
		this.addOffset(sumoffset);
		var constraintoffsetUpper = this.inequalityConstraintUpper();
		var constraintoffsetLower = this.inequalityConstraintLower();
		this.itteration = this.itteration + 1;
		return (constraintoffsetUpper + convergence + constraintoffsetLower + Math.abs(sumoffset));
	}

	/**
	 * Computes the offset from the target value based on the sum of the elements in
	 * the input array.
	 * 
	 * @param val An array of values to be summed.
	 * @return The computed offset from the target value based on the sum of the
	 *         input array.
	 */
	private double sumConstraint(double[] val) {
		double sum = Arrays.stream(val).sum();
		return (sum <= this.setVal || sum >= this.setVal) ? (this.setVal - sum) : 0;
	}

	/**
	 * Computes the offset from the inequality constraints based on the current
	 * point.
	 * 
	 * @return The computed offset from the inequality constraints.
	 */
	private double inequalityConstraintUpper() {
		return IntStream.range(0, this.scale.length)//
				.filter(i -> this.point[i] > this.upper[i])//
				.mapToDouble(i -> {
					double overshoot = this.scale[i] * (this.point[i] - this.upper[i]);
					this.point[i] = this.upper[i];
					return overshoot;
				}).sum();
	}

	/**
	 * Computes the offset from the inequality constraints based on the current
	 * point.
	 * 
	 * @return The computed offset from the inequality constraints.
	 */
	private double inequalityConstraintLower() {
		return IntStream.range(0, this.scale.length)//
				.filter(i -> this.point[i] < this.lower[i])//
				.mapToDouble(i -> {
					double overshoot = this.scale[i] * (this.lower[i] - this.point[i]);
					this.point[i] = this.lower[i];
					return overshoot;
				}).sum();
	}

	/**
	 * Adds the specified offset value to each element of the point array.
	 * 
	 * @param val The offset value to be added to each element of the point array.
	 */
	private void addOffset(double val) {

		double sum = Arrays.stream(this.upper).sum();

		if (this.itteration == 0 || sum == 0) {
			this.point = Arrays.stream(this.point)//
					.map(i -> i + val / this.upper.length)//
					.toArray();
		} else {
			IntStream.range(0, this.point.length)
					.forEach(i -> this.point[i] = this.point[i] + val * this.upper[i] / sum);
		}
	}

}
