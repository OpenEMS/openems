package io.openems.edge.energy.api.handler;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithDifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithOnlyOneMode;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public sealed interface EnergyScheduleHandler permits WithDifferentModes, WithOnlyOneMode {

	/**
	 * Gets the unique ID of this {@link EnergyScheduler}.
	 * 
	 * @return a String identifier
	 */
	public String getId();

	/**
	 * Triggers Rescheduling by the Energy Scheduler.
	 * 
	 * @param reason a reason
	 */
	public void triggerReschedule(String reason);

	/**
	 * Creates a ControllerScheduleContext.
	 * 
	 * @param <SCHEDULE_CONTEXT> the type of the ControllerScheduleContext
	 * @return a new ControllerScheduleContext
	 */
	public <SCHEDULE_CONTEXT> SCHEDULE_CONTEXT createScheduleContext();

	/**
	 * A {@link EnergyScheduleHandler} for {@link EnergySchedulable} OpenEMS
	 * {@link Controller}s that can handle multiple different Modes.
	 */
	public static sealed interface WithDifferentModes extends EnergyScheduleHandler permits EshWithDifferentModes {

		/**
		 * Create a {@link EnergyScheduleHandler.WithDifferentModes} for a
		 * {@link Controller} with different Modes that can be evaluated.
		 *
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param <SCHEDULE_CONTEXT>     the type of the ControllerScheduleContext
		 * @return a {@link DifferentModes.Builder}
		 */
		public static <MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> DifferentModes.Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> create() {
			return new DifferentModes.Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>();
		}

		/**
		 * Gets the index of the default Mode.
		 * 
		 * @return the index of the default Mode
		 */
		public int getDefaultModeIndex();

		/**
		 * Gets the total number of available modes. This is implemented as
		 * Array.length.
		 * 
		 * @return number of available modes
		 */
		public int getNumberOfAvailableModes();

		/**
		 * Gets the string representation for the given modeIndex.
		 * 
		 * @param modeIndex the index of the Mode
		 * @return string representation
		 */
		public String toModeString(int modeIndex);

		/**
		 * Simulates a Mode for one Period of a Schedule.
		 *
		 * @param period    the {@link GlobalOptimizationContext.Period}
		 * @param gsc       the {@link GlobalScheduleContext}
		 * @param csc       the ControllerScheduleContext
		 * @param ef        the {@link EnergyFlow.Model}
		 * @param modeIndex the index of the simulated Mode
		 * @return additional cost to be considered by the cost function
		 */
		public double simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
				EnergyFlow.Model ef, int modeIndex);

		/**
		 * Post-processes a Period of the best Schedule.
		 * 
		 * <p>
		 * This method is called internally after the Simulations are executed with the
		 * found best Schedule.
		 * 
		 * @param period     the {@link GlobalOptimizationContext.Period}
		 * @param gsc        the {@link GlobalScheduleContext}
		 * @param energyFlow the {@link EnergyFlow}
		 * @param modeIndex  the index of the simulated Mode
		 * @return the post-processed Mode index
		 */
		public int postProcessPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc,
				EnergyFlow energyFlow, int modeIndex);

		/**
		 * Applies a new Schedule.
		 * 
		 * <p>
		 * This method is called by the {@link EnergyScheduler}.
		 * 
		 * @param schedule the new Schedule as Map of ZonedDateTime to Mode-Index
		 */
		public void applySchedule(ImmutableSortedMap<ZonedDateTime, DifferentModes.Period.Transition> schedule);
	}

	public static sealed interface WithOnlyOneMode extends EnergyScheduleHandler permits EshWithOnlyOneMode {

		/**
		 * Create a {@link EnergyScheduleHandler.WithOnlyOneMode} for a
		 * {@link Controller} with only a single Mode.
		 *
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param <SCHEDULE_CONTEXT>     the type of the ControllerScheduleContext
		 * @return a {@link OneMode.Builder}
		 */
		public static <OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> OneMode.Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> create() {
			return new OneMode.Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>();
		}

		/**
		 * Simulates one Period of a Schedule.
		 * 
		 * @param period the {@link GlobalOptimizationContext.Period}
		 * @param gsc    the {@link GlobalScheduleContext}
		 * @param csc    the ControllerScheduleContext
		 * @param ef     the {@link EnergyFlow.Model}
		 */
		public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
				EnergyFlow.Model ef);
	}
}
