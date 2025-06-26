package io.openems.edge.energy.api;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public interface EnergySchedulable extends OpenemsComponent {

	/**
	 * Get the {@link EnergyScheduleHandler}.
	 * 
	 * @return {@link EnergyScheduleHandler}
	 */
	public EnergyScheduleHandler getEnergyScheduleHandler();
}
