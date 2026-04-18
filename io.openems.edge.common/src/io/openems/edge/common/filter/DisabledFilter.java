package io.openems.edge.common.filter;

/**
 * A disabled filter, i.e. output = input.
 */
public final class DisabledFilter extends Filter {

	/**
	 * Creates a {@link DisabledFilter}.
	 */
	public DisabledFilter() {
	}

	@Override
	public void reset() {
	}

	/**
	 * Apply the {@link DisabledFilter}.
	 *
	 * @param value the input value
	 * @return the filtered set-point value
	 */
	public int applyDisabledFilter(double value) {
		// apply output value limits
		return this.applyLowHighLimits(value);
	}
}