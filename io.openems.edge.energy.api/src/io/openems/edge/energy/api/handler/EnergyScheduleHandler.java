package io.openems.edge.energy.api.handler;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithDifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithOnlyOneMode;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration;

public sealed interface EnergyScheduleHandler permits WithDifferentModes, WithOnlyOneMode {

	/**
	 * Gets the Factory-PID of the parent {@link OpenemsComponent}.
	 * 
	 * @return a String identifier
	 */
	public String getParentFactoryPid();

	/**
	 * Gets the Component-ID of the parent {@link OpenemsComponent}.
	 * 
	 * @return a String identifier
	 */
	public String getParentId();

	/**
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson();

	/**
	 * Triggers a rescheduling event by the EnergyScheduler.
	 *
	 * @param reason         a descriptive reason for logging/debugging
	 * @param rescheduleMode defines how the current period is handled
	 * @throws NullPointerException if {@code rescheduleMode} is {@code null}
	 */
	public void triggerReschedule(String reason, RescheduleMode rescheduleMode);

	/**
	 * Creates a ControllerScheduleContext.
	 * 
	 * @param <SCHEDULE_CONTEXT> the type of the ControllerScheduleContext
	 * @return a new ControllerScheduleContext
	 */
	public <SCHEDULE_CONTEXT> SCHEDULE_CONTEXT createScheduleContext();

	/**
	 * Gets a copy of the current Schedule.
	 * 
	 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
	 * @return the Schedule
	 */
	public <OPTIMIZATION_CONTEXT> ImmutableSortedMap<ZonedDateTime, ? extends Period<OPTIMIZATION_CONTEXT>> getSchedule();

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
		 * @param parent                 the parent {@link OpenemsComponent}
		 * @return a {@link DifferentModes.Builder}
		 */
		public static <MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> DifferentModes.Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> create(
				OpenemsComponent parent) {
			return new DifferentModes.Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>(parent);
		}

		/**
		 * Gets the {@link Modes} of this
		 * {@link EnergyScheduleHandler.WithDifferentModes}.
		 * 
		 * @return the {@link Modes}
		 */
		public Modes<?> modes();

		/**
		 * Generates {@link InitialPopulation} for this
		 * {@link EnergyScheduleHandler.WithDifferentModes}.
		 * 
		 * @param goc the {@link GlobalOptimizationContext}
		 * @return a List of {@link InitialPopulation}s
		 */
		public ImmutableList<InitialPopulation.Transition> getInitialPopulation(GlobalOptimizationContext goc);

		/**
		 * Pre-Process a Mode of a Period before Simulation, i.e. replace with fixed or
		 * manually planned Mode.
		 * 
		 * @param period    the {@link GlobalOptimizationContext.Period}
		 * @param gsc       the {@link GlobalScheduleContext}
		 * @param modeIndex the index of the simulated Mode
		 * @return the post-processed Mode index
		 */
		public int preProcessPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, int modeIndex);

		/**
		 * Simulates a Mode for one Period of a Schedule.
		 *
		 * @param period     the {@link GlobalOptimizationContext.Period}
		 * @param gsc        the {@link GlobalScheduleContext}
		 * @param csc        the ControllerScheduleContext
		 * @param ef         the {@link EnergyFlow.Model}
		 * @param modeIndex  the index of the simulated Mode; -1 if no Mode is available
		 * @param fitness    the {@link Fitness} result
		 * @param isFinalRun is this the final simulation run?
		 * @return the index of the post-processed Mode
		 */
		public int simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
				EnergyFlow.Model ef, int modeIndex, Fitness fitness, boolean isFinalRun);

		/**
		 * Applies a new Schedule.
		 * 
		 * <p>
		 * This method is called by the {@link EnergyScheduler}.
		 * 
		 * @param schedule the new Schedule as Map of ZonedDateTime to Period
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
		 * @param parent                 the parent {@link OpenemsComponent}
		 * @return a {@link OneMode.Builder}
		 */
		public static <OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> OneMode.Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> create(
				OpenemsComponent parent) {
			return new OneMode.Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>(parent);
		}

		/**
		 * Simulates one Period of a Schedule.
		 * 
		 * @param period  the {@link GlobalOptimizationContext.Period}
		 * @param gsc     the {@link GlobalScheduleContext}
		 * @param csc     the ControllerScheduleContext
		 * @param ef      the {@link EnergyFlow.Model}
		 * @param fitness the {@link Fitness} result
		 */
		public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
				EnergyFlow.Model ef, Fitness fitness);

		/**
		 * Applies a new Schedule.
		 * 
		 * <p>
		 * This method is called by the {@link EnergyScheduler}.
		 * 
		 * @param schedule the new Schedule as Map of ZonedDateTime to Period
		 */
		public void applySchedule(ImmutableSortedMap<ZonedDateTime, OneMode.Period.Transition> schedule);
	}

	public sealed interface Period<OPTIMIZATION_CONTEXT> permits DifferentModes.Period, OneMode.Period {

		/**
		 * Duration of the Period.
		 * 
		 * @return the {@link PeriodDuration}
		 */
		public PeriodDuration duration();

		/**
		 * Price [1/MWh].
		 * 
		 * @return the price per period; possibly null
		 */
		public Double price();

		/**
		 * Simulated {@link EnergyFlow}.
		 * 
		 * @return the EnergyFlow
		 */
		public EnergyFlow energyFlow();

		/**
		 * The ControllerOptimizationContext.
		 * 
		 * @return the ControllerOptimizationContext
		 */
		public OPTIMIZATION_CONTEXT coc();
	}
}
