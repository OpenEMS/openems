package io.openems.edge.energy.api.simulatable;

@FunctionalInterface
public interface Simulator {

	public void simulate(ExecutionPlan.Period period);

}
