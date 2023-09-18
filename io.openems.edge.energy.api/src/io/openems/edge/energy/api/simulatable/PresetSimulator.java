package io.openems.edge.energy.api.simulatable;

import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.simulatable.ExecutionPlan.Period;

@FunctionalInterface
public interface PresetSimulator<PRESET extends Schedule.Preset> extends Simulator {

	/**
	 * Simulate a Period.
	 * 
	 * @param period      the period of the {@link ExecutionPlan}
	 * @param componentId the Component-ID of the simulated parent Component. Used
	 *                    to get the simulated Schedule.Mode.
	 */
	public void simulate(ExecutionPlan.Period period, String componentId);

	@Override
	public default void simulate(Period period) {
		throw new IllegalArgumentException("Must provide a Preset for PresetSimulator");
	}

}
