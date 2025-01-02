package io.openems.edge.energy.api;

import io.openems.edge.common.component.OpenemsComponent;

public interface EnergySchedulable extends OpenemsComponent {

	/**
	 * Get the {@link EnergyScheduleHandler}.
	 * 
	 * @return {@link EnergyScheduleHandler}
	 */
	public EnergyScheduleHandler getEnergyScheduleHandler();
}
