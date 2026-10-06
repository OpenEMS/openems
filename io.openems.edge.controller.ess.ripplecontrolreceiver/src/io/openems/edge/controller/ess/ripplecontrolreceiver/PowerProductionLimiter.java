package io.openems.edge.controller.ess.ripplecontrolreceiver;

public interface PowerProductionLimiter {
	void setMaxNominalProductionPower(int maxNominalProductionPowerInW);

	/**
	 * Returns how much watt we can feed into the grid.
	 *
	 * @return Limit in Wh
	 */
	Integer getGridFeedInLimit();
}
