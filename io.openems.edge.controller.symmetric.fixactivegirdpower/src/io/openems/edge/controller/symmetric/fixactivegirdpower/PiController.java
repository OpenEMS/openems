package io.openems.edge.controller.symmetric.fixactivegirdpower;

/**
 * A proportional-integral controller.
 * 
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/PID_controller">https://en.wikipedia.org/wiki/PID_controller</a>
 */
public class PiController {

	public static final int ERROR_SUM_LIMIT_FACTOR = 10;

	private final double proportionalGain;
	private final double resetTimeInSeconds;
	private final boolean enableIdelay;

	private double errorSum = 0;
	private Integer lowLimit = null;
	private Integer highLimit = null;
	private double cycleTimeInSeconds = 0;

	/**
	 * Creates a PiController.
	 * 
	 * @param proportionalGain		the proportional gain
	 * @param resetTimeInSeconds	the reset time (Nachstellzeit) in seconds
	 * @param enableIdelay 			enables a delay of one cycle time @ integrator
	 */
	public PiController(double proportionalGain, double resetTimeInSeconds, boolean enableIdelay) {
		this.proportionalGain = proportionalGain;
		this.resetTimeInSeconds = resetTimeInSeconds;
		this.enableIdelay = enableIdelay;
	}

	public void setCycleTimeInSeconds(double cycleTimeSeconds) {
		this.cycleTimeInSeconds = cycleTimeSeconds;
	}

	/**
	 * Limit the output value.
	 * 
	 * @param lowLimit  lowest allowed output value
	 * @param highLimit highest allowed output value
	 */
	public void setLimits(Integer lowLimit, Integer highLimit) {
		if (lowLimit != null && highLimit != null && lowLimit > highLimit) {
			throw new IllegalArgumentException(
					"Given LowLimit [" + lowLimit + "] is higher than HighLimit [" + highLimit + "]");
		}
		this.lowLimit = lowLimit;
		this.highLimit = highLimit;
	}

	/**
	 * Apply the PI filter using the current Channel value as input and the target
	 * value.
	 * 
	 * @param measuredOutput the input value, e.g. the measured Channel value
	 * @param reference      the target value
	 * @return the filtered set-point value
	 */
	public int applyPiFilter(int measuredOutput, int reference) {
		// Pre-process the target value: apply output value limits
		reference = this.applyLowHighLimits(reference);

		// Calculate the error
		int error = reference - measuredOutput;

		// Calculate I
		double i = 0.0;
		if (this.resetTimeInSeconds != 0.0) {
			if (this.enableIdelay == false) {
				this.errorSum = this.applyErrorSumLimit(this.errorSum + error);
			}
			i = this.cycleTimeInSeconds / this.resetTimeInSeconds * this.errorSum;
		}
		
		// Sum outputs
		double output = this.proportionalGain * (error + i);

		if ((this.enableIdelay == true) && (this.resetTimeInSeconds != 0.0)) {
			this.errorSum = this.applyErrorSumLimit(this.errorSum + error);
		}

		// Post-process the output value: convert to integer and apply value limits
		return this.applyLowHighLimits(Math.round((float) output));
	}

	/**
	 * Reset the PI filter.
	 * 
	 * <p>
	 * This method should be called when the filter was not used for a while.
	 */
	public void reset() {
		this.errorSum = 0;
	}

	/**
	 * Applies the configured PID low and high limits to a value.
	 * 
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	protected int applyLowHighLimits(int value) {
		if (this.lowLimit != null && value < this.lowLimit) {
			value = this.lowLimit;
		}
		if (this.highLimit != null && value > this.highLimit) {
			value = this.highLimit;
		}
		return value;
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