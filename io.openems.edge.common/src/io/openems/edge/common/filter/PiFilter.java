package io.openems.edge.common.filter;

/**
 * A proportional-integral-derivative controller.
 * 
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/PID_controller">https://en.wikipedia.org/wiki/PID_controller</a>
 */
public class PiFilter {

	private final double kp;
	private final double ti;
	private final double deltaT;
	
	public static final int ERROR_SUM_LIMIT_FACTOR = 10;

	private double errorSum = 0;
	private Integer lowLimit = null;
	private Integer highLimit = null;

	/**
	 * Creates a PidFilter.
	 * 
	 * @param kp ... K_p (gain / Verstärkung)
	 * @param ti ... T_i (reset time / Nachstellzeit)
	 * @param deltaT ... time between two calculations
	 */
	public PiFilter(double kp, double ti, double deltaT) {
		this.kp = kp;
		this.ti = ti;
		this.deltaT = deltaT;
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
	 * Apply the PID filter using the current Channel value as input and the target
	 * value.
	 * 
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 */
	public int applyPiFilter(int measuredOutput, int reference) {

		// Calculate the error
		double error = (double)reference - (double)measuredOutput;
		
		// Calculate I
		double i = 0.0;
		if (this.ti != 0) {
			this.errorSum = this.applyErrorSumLimit(this.errorSum + error);
			i = this.deltaT / this.ti * this.errorSum;
		}

		// Calculate Output
		double output = this.kp * (error + i);

		// Post-process the output value: convert to integer and apply value limits
		return (Math.round((float) output));
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