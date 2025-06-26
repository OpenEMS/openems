package io.openems.edge.common.filter;

/**
 * A controller that applies a ramp to a given value.
 */
public class RampFilter {

	// Last value that was calculated last cycle
	private Float lastValue;

	public RampFilter() {
		this(null);
	}

	public RampFilter(Float initialValue) {
		this.lastValue = initialValue;
	}

	/**
	 * Get filtered value using the present lastValue, the value to reach and a
	 * fixed maximum change per call.
	 *
	 * @param targetValue      Value to reach
	 * @param maxChangePerCall Fixed change per call
	 * @return value as Integer with applied ramp filter
	 */
	public Integer getFilteredValueAsInteger(Float targetValue, float maxChangePerCall) {
		var result = this.applyRampFilter(targetValue, maxChangePerCall);
		if (result == null) {
			return null;
		}
		return Math.round(result);
	}

	/**
	 * Get filtered value using the given lastValue, the value to reach and a
	 * calculated maximum change per call.
	 *
	 * @param targetValue  Value to reach
	 * @param lastValue    Last or current value that needs to be adjusted
	 * @param maximumLimit Maximum limit used to calculate a fixed change per call
	 * @param increaseRate Increasing rate used to calculate a fixed change per call
	 * @return value as Integer with applied ramp filter
	 */
	public Integer getFilteredValueAsInteger(float lastValue, Float targetValue, float maximumLimit,
			float increaseRate) {
		this.lastValue = lastValue;
		return this.getFilteredValueAsInteger(targetValue, maximumLimit * increaseRate);
	}

	/**
	 * Get filtered value using the given lastValue, the value to reach and a fixed
	 * maximum change per call.
	 *
	 * @param targetValue      Value to reach
	 * @param lastValue        Last or current value that needs to be adjusted
	 * @param maxChangePerCall Fixed change per call
	 * @return value as Integer with applied ramp filter
	 */
	public Integer getFilteredValueAsInteger(float lastValue, Float targetValue, float maxChangePerCall) {
		this.lastValue = lastValue;
		return this.getFilteredValueAsInteger(targetValue, maxChangePerCall);
	}

	private Float applyRampFilter(Float targetValue, float maxChangePerCall) {
		if (targetValue == null) {
			return null;
		}

		if (this.lastValue == null) {
			this.lastValue = targetValue;
			return targetValue;
		}
		if (targetValue > this.lastValue) {
			return this.increase(targetValue, maxChangePerCall);
		} else {
			return this.decrease(targetValue, maxChangePerCall);
		}
	}

	private float increase(float targetValue, float maxChangePerCall) {
		final float result;
		if (this.lastValue == null) {
			result = this.lastValue;
		} else {
			result = Math.min(this.lastValue + maxChangePerCall, targetValue);
		}

		this.lastValue = result;
		return result;
	}

	private float decrease(float targetValue, float maxChangePerCall) {
		final float result;
		if (this.lastValue == null) {
			result = targetValue;
		} else {
			result = Math.max(this.lastValue - maxChangePerCall, targetValue);
		}
		this.lastValue = result;
		return result;
	}
}
