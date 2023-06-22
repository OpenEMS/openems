package io.openems.edge.battery.api;

import io.openems.edge.common.startstop.StartStop;

public interface BatteryInhibitable extends Battery {
	/**
	 * A helper in deciding whether to turn on the main contactor of the batteries.
	 * Particularly for parallel clusters, it is necessary. By
	 * {@link StartStop#START} Batteries can be started in order to communicate with
	 * them, but after the voltage comparison decision has been made, the primary
	 * contactor of the second, third... batteries can be closed.
	 * 
	 * @param value true to turn on the main contactor
	 */
	public void setMainContactorUnlocked(boolean value);
}
