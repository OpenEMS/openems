package io.openems.edge.energy.api;

import io.openems.edge.controller.api.Controller;

public interface EnergySchedulable extends Controller {

	/**
	 * Get the {@link EnergyScheduleHandler}.
	 * 
	 * @return {@link EnergyScheduleHandler}
	 */
	public EnergyScheduleHandler getEnergyScheduleHandler();
}
