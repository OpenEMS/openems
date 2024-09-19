package io.openems.edge.energy.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;

public sealed interface EnergyScheduleHandler {

	/**
	 * Creates an {@link EnergyScheduleHandler} for a {@link Controller} with
	 * different states that can be evaluated.
	 * 
	 * @param <STATE>         the type of the State
	 * @param <CONTEXT>       the type of the Context
	 * @param defaultState    the default State if no other is explicitly scheduled
	 * @param statesSupplier  a {@link Supplier} for available States
	 * @param contextFunction a {@link Function} to create a Context
	 * @param simulator       a simulator that modifies a given {@link EnergyFlow}
	 * @return an {@link EnergyScheduleHandler}
	 */
	public static <STATE, CONTEXT> EnergyScheduleHandler.WithDifferentStates<STATE, CONTEXT> of(//
			STATE defaultState, //
			Supplier<STATE[]> statesSupplier, //
			Function<GlobalSimulationsContext, CONTEXT> contextFunction, //
			WithDifferentStates.Simulator<STATE, CONTEXT> simulator) {
		return new EnergyScheduleHandler.WithDifferentStates<STATE, CONTEXT>(defaultState, statesSupplier,
				contextFunction, simulator);
	}

	/**
	 * Creates an {@link EnergyScheduleHandler} for a {@link Controller} with only a
	 * single state.
	 * 
	 * @param <CONTEXT>       the type of the Context
	 * @param contextFunction a {@link Function} to create a Context
	 * @param simulator       a simulator that modifies a given {@link EnergyFlow}
	 * @return an {@link EnergyScheduleHandler}
	 */
	public static <CONTEXT> EnergyScheduleHandler.WithOnlyOneState<CONTEXT> of(//
			Function<GlobalSimulationsContext, CONTEXT> contextFunction, //
			WithOnlyOneState.Simulator<CONTEXT> simulator) {
		return new EnergyScheduleHandler.WithOnlyOneState<CONTEXT>(contextFunction, simulator);
	}

	/**
	 * Triggers Rescheduling by the Energy Scheduler.
	 */
	public void triggerReschedule();

	public abstract static sealed class AbstractEnergyScheduleHandler<CONTEXT> implements EnergyScheduleHandler {

		private final Function<GlobalSimulationsContext, CONTEXT> contextFunction;

		protected Clock clock;
		protected CONTEXT context;
		private Runnable onRescheduleCallback;

		public AbstractEnergyScheduleHandler(Function<GlobalSimulationsContext, CONTEXT> contextFunction) {
			this.contextFunction = contextFunction;
		}

		/**
		 * Initialize the {@link EnergyScheduleHandler}.
		 * 
		 * <p>
		 * This method is called internally before a Simulation is executed.
		 * 
		 * @param asc the {@link GlobalSimulationsContext}
		 */
		public void initialize(GlobalSimulationsContext asc) {
			this.clock = asc.clock();
			this.context = this.contextFunction.apply(asc);
		}

		/**
		 * This method sets the callback for events that require Rescheduling.
		 * 
		 * @param callback the {@link Runnable} callback
		 */
		public synchronized void setOnRescheduleCallback(Runnable callback) {
			this.onRescheduleCallback = callback;
		}

		/**
		 * This method removes the callback.
		 */
		public synchronized void removeOnRescheduleCallback() {
			this.onRescheduleCallback = null;
		}

		@Override
		public void triggerReschedule() {
			var onRescheduleCallback = this.onRescheduleCallback;
			if (onRescheduleCallback != null) {
				onRescheduleCallback.run();
			}
		}

		protected ZonedDateTime getNow() {
			var clock = this.clock;
			if (clock != null) {
				return ZonedDateTime.now(clock);
			}
			return ZonedDateTime.now();
		}

		protected void buildToString(StringBuilder b) {
			var context = this.context;
			if (context != null) {
				b.append("context=").append(context);
			}
		}
	}

	public static final class WithDifferentStates<STATE, CONTEXT> extends AbstractEnergyScheduleHandler<CONTEXT> {

		public static interface Simulator<STATE, CONTEXT> {
			/**
			 * Simulates a Period.
			 *
			 * @param osc     the {@link OneSimulationContext}
			 * @param period  the {@link GlobalSimulationsContext.Period}
			 * @param model   the {@link EnergyFlow.Model}
			 * @param context the Controller Context
			 * @param state   the simulated State
			 */
			public void simulate(OneSimulationContext osc, GlobalSimulationsContext.Period period,
					EnergyFlow.Model model, CONTEXT context, STATE state);
		}

		private final STATE defaultState;
		private final Supplier<STATE[]> availableStatesSupplier;
		private final Simulator<STATE, CONTEXT> simulator;
		private final SortedMap<ZonedDateTime, Period<STATE, CONTEXT>> schedule = new TreeMap<>();

		private STATE[] availableStates;

		private WithDifferentStates(//
				STATE defaultState, //
				Supplier<STATE[]> availableStatesSupplier, //
				Function<GlobalSimulationsContext, CONTEXT> contextFunction, //
				Simulator<STATE, CONTEXT> simulator) {
			super(contextFunction);
			this.defaultState = defaultState;
			this.availableStatesSupplier = availableStatesSupplier;
			this.simulator = simulator;
		}

		@Override
		public void initialize(GlobalSimulationsContext asc) {
			super.initialize(asc);
			this.availableStates = this.availableStatesSupplier.get();
		}

		/**
		 * Gets the default State.
		 * 
		 * @return the default State
		 */
		public STATE getDefaultState() {
			return this.defaultState;
		}

		/**
		 * Gets the index of the default State.
		 * 
		 * @return the index of the default State
		 */
		public int getDefaultStateIndex() {
			var states = this.availableStates;
			if (states == null) {
				throw new IllegalAccessError(
						"EnergySchedulerHandler is uninitialized. `initialize()` must be called first.");
			}
			return IntStream.range(0, states.length) //
					.filter(i -> states[i] == this.defaultState) //
					.findFirst() //
					.orElse(0 /* fallback */);
		}

		/**
		 * Gets the available States.
		 * 
		 * @return an Array of States
		 */
		public STATE[] getAvailableStates() {
			return this.availableStates;
		}

		/**
		 * Simulates a Period.
		 * 
		 * @param osc        the {@link OneSimulationContext}
		 * @param period     the simulated {@link GlobalSimulationsContext.Period}
		 * @param model      the {@link EnergyFlow.Model}
		 * @param stateIndex the index of the simulated state
		 */
		public void simulatePeriod(OneSimulationContext osc, GlobalSimulationsContext.Period period,
				EnergyFlow.Model model, int stateIndex) {
			this.simulator.simulate(osc, period, model, this.context, this.availableStates[stateIndex]);
		}

		public static record Period<STATE, CONTEXT>(
				/** STATE of the Period */
				STATE state,
				/** Price [1/MWh] */
				double price, //
				/** EnergyScheduleHandler Context */
				CONTEXT context,
				/** Simulated EnergyFlow */
				EnergyFlow energyFlow,
				/** the initial ESS energy in the beginning of the period in [Wh] */
				int essInitialEnergy) {

			/**
			 * This class is only used internally to apply the Schedule.
			 */
			public static record Transition(int stateIndex, double price, EnergyFlow energyFlow, int essInitialEnergy) {
			}

			/**
			 * Builds a {@link EnergyScheduleHandler.WithDifferentStates.Period} from a
			 * {@link EnergyScheduleHandler.WithDifferentStates.Period.Transition} record.
			 * 
			 * @param <STATE>   the type of the State
			 * @param <CONTEXT> the type of the Context
			 * @param t         the
			 *                  {@link EnergyScheduleHandler.WithDifferentStates.Period.Transition}
			 *                  record
			 * @param getState  a method to translate a 'stateIndex' to a STATE
			 * @param context   the CONTEXT used during simulation
			 * @return a {@link Period} record
			 */
			public static <STATE, CONTEXT> Period<STATE, CONTEXT> fromTransitionRecord(Period.Transition t,
					IntFunction<STATE> getState, CONTEXT context) {
				return new Period<>(getState.apply(t.stateIndex), t.price, context, t.energyFlow, t.essInitialEnergy);
			}
		}

		/**
		 * Applies a new Schedule.
		 * 
		 * <p>
		 * This method is called by the {@link EnergyScheduler}.
		 * 
		 * @param schedule the new Schedule as Map of ZonedDateTime to State-Index
		 */
		public void applySchedule(ImmutableSortedMap<ZonedDateTime, Period.Transition> schedule) {
			final var thisQuarter = roundDownToQuarter(this.getNow());
			final var nextQuarter = thisQuarter.plusMinutes(15);
			final var currentContext = this.context;
			synchronized (this.schedule) {
				// Clear outdated entries
				this.schedule.headMap(thisQuarter).clear();

				// Remove future entries
				this.schedule.tailMap(nextQuarter).clear();

				// Update entries from param
				var states = this.availableStates;
				if (states.length == 0) {
					System.err.println("States is empty!"); // TODO proper log
					return;
				}
				schedule //
						.tailMap(this.schedule.isEmpty() //
								? thisQuarter // schedule is empty -> add also this quarter
								: nextQuarter) // otherwise -> start with next quarter
						.forEach((k, t) -> {
							this.schedule.put(k, Period.fromTransitionRecord(t, this::getState, currentContext));
						});
			}
		}

		/**
		 * Gets a copy of the current Schedule.
		 * 
		 * @return the Schedule
		 */
		public ImmutableSortedMap<ZonedDateTime, Period<STATE, CONTEXT>> getSchedule() {
			synchronized (this.schedule) {
				return ImmutableSortedMap.copyOfSorted(this.schedule);
			}
		}

		/**
		 * Gets the current {@link Period} record.
		 *
		 * @return the record of the currently scheduled Period; possibly null
		 */
		public Period<STATE, CONTEXT> getCurrentPeriod() {
			synchronized (this.schedule) {
				final var thisQuarter = roundDownToQuarter(this.getNow());
				return this.schedule.get(thisQuarter);
			}
		}

		/**
		 * Gets the string representation for the given stateIndex.
		 * 
		 * @param stateIndex the index of the state
		 * @return string representation
		 */
		public String toStateString(int stateIndex) {
			return this.getState(stateIndex).toString();
		}

		/**
		 * Gets the STATE for the given stateIndex.
		 * 
		 * @param stateIndex the stateIndex
		 * @return the STATE
		 */
		private STATE getState(int stateIndex) {
			var states = this.availableStates;
			return stateIndex < states.length //
					? states[stateIndex] //
					: this.defaultState;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder() //
					.append("EnergyScheduleHandler.WithDifferentStates ["); //
			var availableStates = this.availableStates;
			if (availableStates != null) {
				b.append("availableStates=").append(Arrays.toString(availableStates)).append(", ");
			}
			super.buildToString(b);
			return b.append("]").toString();
		}
	}

	public static final class WithOnlyOneState<CONTEXT> extends AbstractEnergyScheduleHandler<CONTEXT> {

		public static interface Simulator<CONTEXT> {
			/**
			 * Simulates a Period.
			 *
			 * @param osc     the {@link OneSimulationContext}
			 * @param period  the {@link GlobalSimulationsContext.Period}
			 * @param model   the {@link EnergyFlow.Model}
			 * @param context the Controller Context
			 */
			public void simulate(OneSimulationContext osc, GlobalSimulationsContext.Period period,
					EnergyFlow.Model model, CONTEXT context);
		}

		private final Simulator<CONTEXT> simulator;

		private WithOnlyOneState(//
				Function<GlobalSimulationsContext, CONTEXT> contextFunction, //
				Simulator<CONTEXT> simulator) {
			super(contextFunction);
			this.simulator = simulator;
		}

		/**
		 * Simulates a Period.
		 * 
		 * @param simContext the {@link OneSimulationContext}
		 * @param period     the {@link GlobalSimulationsContext.Period}
		 * @param model      the {@link EnergyFlow.Model}
		 */
		public void simulatePeriod(OneSimulationContext simContext, GlobalSimulationsContext.Period period,
				EnergyFlow.Model model) {
			this.simulator.simulate(simContext, period, model, this.context);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder() //
					.append("EnergyScheduleHandler.WithOnlyOneState ["); //
			super.buildToString(b);
			return b.append("]").toString();
		}
	}
}
