//package io.openems.edge.energy.api.simulatable;
//
//import io.openems.edge.energy.api.ExecutionPlan;
//import io.openems.edge.energy.api.ExecutionPlan.Period;
//import io.openems.edge.energy.api.schedulable.Schedule;
//
//@FunctionalInterface
//// CHECKSTYLE:OFF
//public interface ModeSimulator<MODE extends Schedule.Mode> extends Simulator {
//	// CHECKSTYLE:ON
//
//	/**
//	 * Simulate a Period.
//	 * 
//	 * @param period      the period of the {@link ExecutionPlan}
//	 * @param componentId the Component-ID of the simulated parent Component. Used
//	 *                    to get the simulated Schedule.Mode.
//	 */
//	public void simulate(ExecutionPlan.Period period, String componentId);
//
//	@Override
//	public default void simulate(Period period) {
//		throw new IllegalArgumentException("Must provide a Mode for ModeSimulator");
//	}
//
//}
