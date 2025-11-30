package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithDifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithOnlyOneMode;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

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
	 * Gets a copy of the current Schedule.
	 * 
	 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
	 * @return the Schedule
	 */
	public <OPTIMIZATION_CONTEXT> ImmutableSortedMap<ZonedDateTime, ? extends Period<OPTIMIZATION_CONTEXT>> getSchedule();

	public static class Fitness implements Comparable<Fitness> {

		private int hardConstraintViolations = 0;
		private double gridBuyCost = 0.;
		private double gridSellRevenue = 0.;

		/**
		 * Gets the number of Hard-Constraint-Violations.
		 * 
		 * @return Hard-Constraint-Violations
		 */
		public int getHardConstraintViolations() {
			return this.hardConstraintViolations;
		}

		/**
		 * Add a Hard-Constraint-Violation with degree=1.
		 */
		public void addHardConstraintViolation() {
			this.hardConstraintViolations++;
		}

		/**
		 * Add a Hard-Constraint-Violation.
		 * 
		 * @param degree degree of violation
		 */
		public void addHardConstraintViolation(int degree) {
			this.hardConstraintViolations += degree;
		}

		/**
		 * Gets the Grid-Buy cost.
		 * 
		 * @return Grid-Buy cost
		 */
		public double getGridBuyCost() {
			return this.gridBuyCost;
		}

		/**
		 * Add Grid-Buy cost.
		 * 
		 * @param cost the cost
		 */
		public void addGridBuyCost(double cost) {
			this.gridBuyCost += cost;
		}

		/**
		 * Add Grid-Sell revenue.
		 * 
		 * @param revenue the revenue
		 */
		public void addGridSellRevenue(double revenue) {
			this.gridSellRevenue += revenue;
		}

		@Override
		public int compareTo(Fitness o) {
			// 1st priority: hard constraints (lower is better)
			if (this.hardConstraintViolations != o.hardConstraintViolations) {
				return Integer.compare(this.hardConstraintViolations, o.hardConstraintViolations);
			}

			// 2nd priority: grid buy cost (lower is better)
			if (this.gridBuyCost != o.gridBuyCost) {
				return Double.compare(this.gridBuyCost, o.gridBuyCost);
			}

			// 3nd priority: grid sell revenue (higher is better)
			return Double.compare(o.gridSellRevenue, this.gridSellRevenue);
		}

		@Override
		public String toString() {
			return toStringHelper(Fitness.class) //
					.add("hardConstraintViolations", this.hardConstraintViolations) //
					.add("gridBuyCost", this.gridBuyCost) //
					.add("gridSellRevenue", this.gridSellRevenue) //
					.toString();
		}
	}

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
		 * Generates {@link InitialPopulation} for this
		 * {@link EnergyScheduleHandler.WithDifferentModes}.
		 * 
		 * @param goc the {@link GlobalOptimizationContext}
		 * @return a List of {@link InitialPopulation}s
		 */
		public ImmutableList<InitialPopulation.Transition> getInitialPopulation(GlobalOptimizationContext goc);

		/**
		 * Simulates a Mode for one Period of a Schedule.
		 *
		 * @param period    the {@link GlobalOptimizationContext.Period}
		 * @param gsc       the {@link GlobalScheduleContext}
		 * @param csc       the ControllerScheduleContext
		 * @param ef        the {@link EnergyFlow.Model}
		 * @param modeIndex the index of the simulated Mode
		 * @param fitness   the {@link Fitness} result
		 */
		public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
				EnergyFlow.Model ef, int modeIndex, Fitness fitness);

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
		 * @return the price per period
		 */
		public double price();

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
