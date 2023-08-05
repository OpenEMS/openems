package io.openems.edge.energy.api.simulatable;

import io.openems.edge.energy.api.ExecutionPlan;

@FunctionalInterface
public interface Simulator {

	/**
	 * Simulate a Period of an {@link ExecutionPlan}.
	 * 
	 * @param period the period
	 */
	public void simulate(ExecutionPlan.Period period);

}
