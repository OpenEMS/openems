package io.openems.edge.common.filter;

public sealed abstract class Filter permits PidFilter, LowPassFilter {

	protected Integer lowLimit = null;
	protected Integer highLimit = null;

	protected Filter() {
	}

	/**
	 * Limit the output value.
	 *
	 * @param lowLimit  lowest allowed output value
	 * @param highLimit highest allowed output value
	 * @throws IllegalArgumentException if lowLimit > highLimit
	 */
	public void setLimits(Integer lowLimit, Integer highLimit) throws IllegalArgumentException {
		if (lowLimit != null && highLimit != null && lowLimit > highLimit) {
			throw new IllegalArgumentException(
					"Given LowLimit [" + lowLimit + "] is higher than HighLimit [" + highLimit + "]");
		}
		this.lowLimit = lowLimit;
		this.highLimit = highLimit;
	}

	/**
	 * Applies the configured low and high limits to a int value.
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
	 * Applies the configured low and high limits to a double value.
	 *
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	protected int applyLowHighLimits(double value) {
		return this.applyLowHighLimits(Math.round((float) value));
	}
}
