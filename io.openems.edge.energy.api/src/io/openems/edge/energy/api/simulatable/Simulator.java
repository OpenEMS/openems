package io.openems.edge.energy.api.simulatable;

@FunctionalInterface
public interface Simulator {

	/**
	 * Execute the Simulation.
	 * 
	 * @param period one Period of the {@link ExecutionPlan}.
	 */
	public void simulate(ExecutionPlan.Period period);

}
