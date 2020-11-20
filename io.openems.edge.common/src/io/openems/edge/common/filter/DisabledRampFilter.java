package io.openems.edge.common.filter;

/**
 * This implementation ignores the Ramp filter and instead just returns the
 * unfiltered target value - making sure it is within the allowed minimum and
 * maximum limits. It is used when the using Component is configured to disable
 * Ramp filter.
 */
public class DisabledRampFilter extends RampFilter {

	@Override
	public int applyRampFilter(int input, int target) {
		return this.applyLowHighLimits(target);
	}
}
