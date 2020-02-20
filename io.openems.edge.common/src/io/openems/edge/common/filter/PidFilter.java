package io.openems.edge.common.filter;

/**
 * A proportional-integral-derivative controller.
 * 
 * @see https://en.wikipedia.org/wiki/PID_controller
 */
public class PidFilter {

	public static final double DEFAULT_P = 0.5;
	public static final double DEFAULT_I = 0.2;
	public static final double DEFAULT_D = 0.1;

	private final double p;
	private final double i;
	private final double d;

	private boolean firstRun = true;

	private double lastOutput = 0;
	private double lastInput = 0;

	private double errorMax = 0;
	private double errorSum = 0;

	private double lowLimit = 0;
	private double highLimit = 0;
	private double highLimitForI = 0;
	private double outputFilter = 0;
	private double rampLimit = 0;
	private double targetDistanceLimit = 0;

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
	 * Limit the output value.
	 * 
	 * @param lowLimit  lowest allowed output value
	 * @param highLimit highest allowed output value
	 */
	public void setLimits(double lowLimit, double highLimit) {
		if (highLimit < lowLimit) {
			return;
		}
		// Apply limit for I
		if (this.highLimitForI == 0 //
				|| this.highLimitForI > (highLimit - lowLimit)) {
			this.setHighLimitForI(highLimit - lowLimit);
		}
		// Apply general limits
		this.lowLimit = lowLimit;
		this.highLimit = highLimit;
	}

	/**
	 * Limit the max value of I to avoid windup. This is internally set by
	 * {@link PidFilter}{@link #setLimits(double, double)}.
	 * 
	 * @param highLimitForI the high limit for I
	 */
	public void setHighLimitForI(double highLimitForI) {
		this.highLimitForI = highLimitForI;
		if (this.i != 0) {
			this.errorMax = highLimitForI / this.i;
		}
	}

	/**
	 * Applies a limit on the maximum distance the target value is allowed to have
	 * from the input value.
	 * 
	 * @param targetDistanceLimit the target distance limit
	 */
	public void setTargetDistanceLimit(double targetDistanceLimit) {
		this.targetDistanceLimit = targetDistanceLimit;
	}

	/**
	 * Apply the PID filter using the current Channel value as input and the target
	 * value.
	 * 
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 */
	public double applyPidFilter(double input, double target) {
		// Applies a limit on the maximum distance the target value is allowed to have
		// from the input value.
		if (this.targetDistanceLimit != 0) {
			target = this.applyLowHighLimits(target, input - this.targetDistanceLimit,
					input + this.targetDistanceLimit);
		}

		// Calculate the error
		double error = target - input;

		// Calculate P
		double outputP = this.p * error;

		// Set last values on first run
		if (this.firstRun) {
			this.lastInput = input;
			this.lastOutput = outputP;
			this.firstRun = false;
		}

		// Calculate I
		double outputI = this.i * this.errorSum;
		if (this.highLimitForI != 0) {
			// Limit the max value of I to avoid windup
			outputI = this.applyLowHighLimits(outputI, -this.highLimitForI, this.highLimitForI);
		}

		// Calculate D
		double outputD = -this.d * (input - this.lastInput);

		// Store last input value
		this.lastInput = input;

		// Sum outputs
		double output = outputP + outputI + outputD;

		/*
		 * Calculate error
		 */
		if (this.highLimit != this.lowLimit && !this.isWithinLimits(output, this.highLimit, this.lowLimit)) {
			// Output value does not fit into low and high limits -> reset error
			this.errorSum = error;

		} else if (this.rampLimit != 0
				&& !this.isWithinLimits(output, this.lastOutput - this.rampLimit, this.lastOutput + this.rampLimit)) {
			// Output value does not fit into the limit on the rate at which the output
			// value can increase -> reset error
			this.errorSum = error;

		} else if (this.highLimitForI != 0) {
			// Limit the max value of I -> prevent windup.
			this.errorSum = this.applyLowHighLimits(this.errorSum + error, -this.errorMax, this.errorMax);

		} else {
			// Regular sum up of the error
			this.errorSum += error;
		}

		/*
		 * Post-process the output value
		 */
		if (this.highLimit != this.lowLimit) {
			// Apply output value limits
			output = this.applyLowHighLimits(output, this.lowLimit, this.highLimit);
		}

		if (this.rampLimit != 0) {
			// Apply ramp rate limit
			output = this.applyLowHighLimits(output, this.lastOutput - this.rampLimit,
					this.lastOutput + this.rampLimit);
		}

		if (this.outputFilter != 0) {
			// Apply additional output filter
			output = this.lastOutput * this.outputFilter + output * (1 - this.outputFilter);
		}

		// Store last output value
		this.lastOutput = output;

		return output;
	}

	/**
	 * Reset the PID filter.
	 * 
	 * <p>
	 * This method should be called when the filter was not used for a while.
	 */
	public void reset() {
		this.errorSum = 0;
		this.firstRun = true;
	}

	/**
	 * Applies a limit on the rate at which the output value can increase.
	 * 
	 * @param rampLimit the rampLimit
	 */
	public void setRampLimit(double rampLimit) {
		this.rampLimit = rampLimit;
	}

	/**
	 * Applies an additional filter on the output.
	 * 
	 * @param outputFilter the output filter
	 */
	public void setOutputFilter(double outputFilter) {
		this.outputFilter = outputFilter;
	}

	/**
	 * Applies the low and high limits to a value.
	 * 
	 * @param value the input value
	 * @param low   low limit
	 * @param high  high limit
	 * @return the value within low and high limit
	 */
	private double applyLowHighLimits(double value, double low, double high) {
		if (value < low) {
			value = low;
		}
		if (value > high) {
			value = high;
		}
		return value;
	}

	/**
	 * Checks whether the value is within low and high limits.
	 * 
	 * @param value     the input value
	 * @param lowLimit  low limit
	 * @param highLimit high limit
	 * @return true if the value is within low and high limits.
	 */
	private boolean isWithinLimits(double value, double lowLimit, double highLimit) {
		return (value < highLimit) && (lowLimit < value);
	}
}