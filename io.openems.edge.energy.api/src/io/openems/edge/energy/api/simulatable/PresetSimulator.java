package io.openems.edge.energy.api.simulatable;

@FunctionalInterface
public interface PresetSimulator {

	/**
	 * Simulate a Period.
	 * 
	 * @param period      the period of the {@link ExecutionPlan}
	 * @param componentId the Component-ID of the simulated parent Component. Used
	 *                    to get the simulated Schedule.Preset. TODO
	 *                    notwendig/besser/Context?
	 */
	public void simulate(ExecutionPlan.Period period, String componentId);

}
