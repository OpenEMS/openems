package io.openems.edge.common.filter;

/**
 * This implementation ignores the PID filter and instead just returns the
 * unfiltered target value - making sure it is within the allowed minimum and
 * maximum limits. It is used when {@link PowerComponent} is configured to
 * disable PID filter.
 */
public final class DisabledPidFilter extends PidFilter {

	public static final DisabledPidFilter INSTANCE = new DisabledPidFilter();

	private DisabledPidFilter() {
	}

	@Override
	public int applyPidFilter(int input, int target) {
		return this.applyLowHighLimits(target);
	}

}
