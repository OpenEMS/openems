package io.openems.edge.ess.offgrid.api;

import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Represents a Energy Storage System with Off-Grid capabilities.
 */
public interface OffGridEss extends ManagedSymmetricEss {

	/**
	 * Gets a boolean if the ess is able to build a micro-grid in off-grid.
	 *
	 * <p>
	 * Returns false if the ess is not able to build a micro-grid.
	 *
	 * @return is managed or not
	 */
	public boolean isOffGridPossible();

}
