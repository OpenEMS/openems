package io.openems.edge.common.filter;

import io.openems.common.exceptions.OpenemsException;

/**
 * A controller that increases the input by a given increase rate.
 */
public class RampFilter {

	public static final double DEFAULT_INCREASE_RATE = 0.05;

	private double increasingRate;

	private Integer lowLimit = null;
	private Integer highLimit = null;

	/**
	 * Creates a RampFilter
	 * 
	 * @param increasingRate the rate of increase
	 */
	public RampFilter(double increasingRate) {
		this.increasingRate = DEFAULT_INCREASE_RATE;
		if (increasingRate > 0 && increasingRate < 1) {
			this.increasingRate = increasingRate;
		}
	}

	/**
	 * Creates a RampFilter using default values
	 */
	public RampFilter() {
		this(DEFAULT_INCREASE_RATE);
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
	 * Apply the filter using the current Channel value as input and the target
	 * value.
	 * 
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 * @throws OpenemsException
	 */
	public int applyRampFilter(int input, int target) throws OpenemsException {

		// Pre-process the target value: apply output value limits
		target = this.applyLowHighLimits(target);

		// Make sure that there is a highLimit set
		if (this.highLimit == null) {
			throw new OpenemsException(
					"No high limit given in EvcsFilter. Please call setLimits bevore applying the filter.");
		}

		// We are already there
		if (input == target) {
			return target;
		}

		// Calculate the next additional power
		double additionalPower = this.highLimit * this.increasingRate;

		// Next power
		double output = input;
		if (input < target) {
			// input should increase
			output += additionalPower;
			output = output > target ? target : output;
		} else {
			// input should decrease
			output -= additionalPower;
			output = output < target ? target : output;
		}

		// Post-process the output value: convert to integer
		return Math.round((float) output);
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
}
