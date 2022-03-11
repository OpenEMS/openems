package io.openems.edge.common.filter;

/**
 * This implementation ignores the Ramp filter and instead just returns the
 * unfiltered target value. It is used when the using Component is configured to
 * disable Ramp filter.
 */
public class DisabledRampFilter extends RampFilter {

	@Override
	public Integer getFilteredValueAsInteger(Float targetValue, float maxChangePerCall) {
		return this.getAsInt(targetValue);
	}

	@Override
	public Integer getFilteredValueAsInteger(float lastValue, Float targetValue, float maxChangePerCall) {
		return this.getAsInt(targetValue);
	}

	@Override
	public Integer getFilteredValueAsInteger(float lastValue, Float targetValue, float maximumLimit,
			float increasingRate) {
		return this.getAsInt(targetValue);
	}

	private Integer getAsInt(Float value) {
		if (value == null) {
			return null;
		}
		return Math.round(value);
	}
}
