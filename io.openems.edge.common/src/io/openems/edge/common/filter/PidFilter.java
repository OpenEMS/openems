package io.openems.edge.common.filter;

/**
 * A proportional-integral-derivative controller.
 *
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/PID_controller">https://en.wikipedia.org/wiki/PID_controller</a>
 */
public non-sealed class PidFilter extends Filter {

	public static final double DEFAULT_P = 0.3;
	public static final double DEFAULT_I = 0.3;
	public static final double DEFAULT_D = 0.1;

	public static final int ERROR_SUM_LIMIT_FACTOR = 10;

	private final double p;
	private final double i;
	private final double d;

	private boolean firstRun = true;

	private double lastInput = 0;
	private double errorSum = 0;

	/**
	 * Creates a PidFilter.
	 *
	 * @param p the proportional gain
	 * @param i the integral gain
	 * @param d the derivative gain
	 */
	public PidFilter(double p, double i, double d) {
		this.p = p;
		this.i = i;
		this.d = d;
	}

	/**
	 * Creates a PidFilter using default values.
	 */
	public PidFilter() {
		this(DEFAULT_P, DEFAULT_I, DEFAULT_D);
	}

	/**
	 * Apply the PID filter using the current Channel value as input and the target
	 * value.
	 *
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 */
	public int applyPidFilter(int input, int target) {
		// Pre-process the target value: apply output value limits
		target = this.applyLowHighLimits(target);

		// Calculate the error
		var error = target - input;

		// We are already there
		if (error == 0) {
			return target;
		}

		// Calculate P
		var outputP = this.p * error;

		// Set last values on first run
		if (this.firstRun) {
			this.lastInput = input;
			this.firstRun = false;
		}

		// Calculate I
		var outputI = this.i * this.errorSum;

		// Calculate D
		var outputD = -this.d * (input - this.lastInput);

		// Store last input value
		this.lastInput = input;

		// Sum outputs
		var output = outputP + outputI + outputD;

		// Sum up the error and limit Error-Sum to not grow too much. Otherwise the PID
		// filter will stop reacting on changes properly.
		this.errorSum = this.applyErrorSumLimit(this.errorSum + error);

		// Post-process the output value: convert to integer and apply value limits
		return this.applyLowHighLimits(output);
	}

	/**
	 * Applies the low and high limits to the error sum.
	 *
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	private double applyErrorSumLimit(double value) {
		// find (positive) limit from low & high limits
		double errorSumLimit;
		if (this.lowLimit != null && this.highLimit != null) {
			errorSumLimit = Math.max(Math.abs(this.lowLimit), Math.abs(this.highLimit));
		} else if (this.lowLimit != null) {
			errorSumLimit = Math.abs(this.lowLimit);
		} else if (this.highLimit != null) {
			errorSumLimit = Math.abs(this.highLimit);
		} else {
			return value;
		}

		// apply additional factor to increase limit
		errorSumLimit *= ERROR_SUM_LIMIT_FACTOR;

		// apply limit
		if (value < errorSumLimit * -1) {
			return errorSumLimit * -1;
		}
		if (value > errorSumLimit) {
			return errorSumLimit;
		}
		return value;
	}
}