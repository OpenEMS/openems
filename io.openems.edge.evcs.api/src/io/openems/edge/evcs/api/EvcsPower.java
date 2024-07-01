package io.openems.edge.evcs.api;

import io.openems.edge.common.filter.RampFilter;

public interface EvcsPower {

	/**
	 * Gets the RampFilter instance with the configured variables.
	 *
	 * @return an instance of {@link RampFilter}
	 */
	public RampFilter getRampFilter();

	/**
	 * Gets the current increase rate.
	 *
	 * @return increase rate
	 */
	public float getIncreaseRate();
}
