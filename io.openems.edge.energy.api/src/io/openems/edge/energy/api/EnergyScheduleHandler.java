package io.openems.edge.energy.api;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;

public sealed interface EnergyScheduleHandler {

	/**
	 * Triggers Rescheduling by the Energy Scheduler.
	 * 
	 * @param reason a reason
	 */
	public void triggerReschedule(String reason);

	public abstract static sealed class AbstractEnergyScheduleHandler<CONTEXT> implements EnergyScheduleHandler {

		private final Function<GlobalSimulationsContext, CONTEXT> contextFunction;

		protected Clock clock;
		protected CONTEXT context;
		private Consumer<String> onRescheduleCallback;

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
		 * @param callback the {@link Consumer} callback with a reason
		 */
		public synchronized void setOnRescheduleCallback(Consumer<String> callback) {
			this.onRescheduleCallback = callback;
		}

		/**
		 * This method removes the callback.
		 */
		public synchronized void removeOnRescheduleCallback() {
			this.onRescheduleCallback = null;
		}

		@Override
		public void triggerReschedule(String reason) {
			var onRescheduleCallback = this.onRescheduleCallback;
			if (onRescheduleCallback != null) {
				onRescheduleCallback.accept(reason);
			}
		}

		protected ZonedDateTime getNow() {
			var clock = this.clock;
			if (clock != null) {
				return ZonedDateTime.now(clock);
			}
			return ZonedDateTime.now();
		}

		protected void buildToString(MoreObjects.ToStringHelper toStringHelper) {
			var context = this.context;
			if (context != null) {
				toStringHelper.addValue(context);
			}
		}
	}

	public static final class WithDifferentStates<STATE, CONTEXT> extends AbstractEnergyScheduleHandler<CONTEXT> {

		public static final class Builder<STATE, CONTEXT> {

			private STATE defaultState;
			private Supplier<STATE[]> availableStatesSupplier;
			private Function<GlobalSimulationsContext, CONTEXT> contextFunction;
			private Function<GlobalSimulationsContext, List<InitialPopulation<STATE>>> initialPopulationsFunction = gsc -> List
					.of();
			private WithDifferentStates.Simulator<STATE, CONTEXT> simulator;
			private WithDifferentStates.PostProcessor<STATE, CONTEXT> postProcessor = PostProcessor.doNothing();

			/**
			 * Sets the default State if no other is explicitly scheduled.
			 * 
			 * @param state a state
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setDefaultState(STATE state) {
				this.defaultState = state;
				return this;
			}

			/**
			 * Sets a {@link Function} to create a {@link GlobalSimulationsContext}.
			 * 
			 * @param contextFunction the context function
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setContextFunction(
					Function<GlobalSimulationsContext, CONTEXT> contextFunction) {
				this.contextFunction = contextFunction;
				return this;
			}

			/**
			 * Sets a {@link Function} to provide {@link InitialPopulation}s.
			 * 
			 * @param initialPopulationsFunction the function
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setInitialPopulationsFunction(
					Function<GlobalSimulationsContext, List<InitialPopulation<STATE>>> initialPopulationsFunction) {
				this.initialPopulationsFunction = initialPopulationsFunction;
				return this;
			}

			/**
			 * Sets a {@link Supplier} for available States.
			 * 
			 * @param supplier a states supplier
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setAvailableStates(Supplier<STATE[]> supplier) {
				this.availableStatesSupplier = supplier;
				return this;
			}

			/**
			 * Sets a {@link WithDifferentStates.Simulator} that modifies a given
			 * {@link EnergyFlow}.
			 * 
			 * @param simulator a simulator
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setSimulator(WithDifferentStates.Simulator<STATE, CONTEXT> simulator) {
				this.simulator = simulator;
				return this;
			}

			/**
			 * Sets a {@link PostProcessor}.
			 * 
			 * @param postProcessor a {@link PostProcessor}
			 * @return myself
			 */
			public Builder<STATE, CONTEXT> setPostProcessor(
					WithDifferentStates.PostProcessor<STATE, CONTEXT> postProcessor) {
				this.postProcessor = postProcessor;
				return this;
			}

			/**
			 * Builds the {@link EnergyScheduleHandler.WithDifferentStates} instance.
			 *
			 * @return a {@link EnergyScheduleHandler.WithDifferentStates}
			 */
			public WithDifferentStates<STATE, CONTEXT> build() {
				return new EnergyScheduleHandler.WithDifferentStates<STATE, CONTEXT>(this.defaultState,
						this.availableStatesSupplier, this.contextFunction, this.initialPopulationsFunction,
						this.simulator, this.postProcessor);
			}
		}

		/**
		 * Create a {@link EnergyScheduleHandler.WithDifferentStates} for a
		 * {@link Controller} with different states that can be evaluated.
		 *
		 * @param <STATE>   the type of the State
		 * @param <CONTEXT> the type of the Context
		 * @return a {@link Builder}
		 */
		public static <STATE, CONTEXT> Builder<STATE, CONTEXT> create() {
			return new Builder<STATE, CONTEXT>();
		}

		public static interface Simulator<STATE, CONTEXT> {
			/**
			 * Simulates a Period.
			 *
			 * @param osc     the {@link OneSimulationContext}
			 * @param period  the {@link GlobalSimulationsContext.Period}
			 * @param model   the {@link EnergyFlow.Model}
			 * @param context the Controller Context
			 * @param state   the simulated State
			 * @return additional cost to be considered by the cost function
			 */
			public double simulate(OneSimulationContext osc, GlobalSimulationsContext.Period period,
					EnergyFlow.Model model, CONTEXT context, STATE state);
		}

		public static interface PostProcessor<STATE, CONTEXT> {

			/**
			 * A 'do-nothing' {@link PostProcessor}.
			 * 
			 * @param <STATE>   the type of the State
			 * @param <CONTEXT> the type of the Context
			 * @return the same State
			 */
			public static <STATE, CONTEXT> PostProcessor<STATE, CONTEXT> doNothing() {
				return (osc, energyFlow, context, state) -> state;
			}

			/**
			 * Post-Process a state of a Period during Simulation, i.e. replace with
			 * 'better' state with the equivalent behaviour.
			 * 
			 * <p>
			 * NOTE: heavy computation is ok here, because this method is called only at the
			 * end with the best Schedule.
			 * 
			 * @param osc        the {@link OneSimulationContext}
			 * @param energyFlow the {@link EnergyFlow}
			 * @param context    the Controller Context
			 * @param state      the initial state
			 * @return the new state
			 */
			public STATE postProcess(OneSimulationContext osc, EnergyFlow energyFlow, CONTEXT context, STATE state);
		}

		private final STATE defaultState;
		private final Supplier<STATE[]> availableStatesSupplier;
		private final Function<GlobalSimulationsContext, List<InitialPopulation<STATE>>> initialPopulationsFunction;
		private final Simulator<STATE, CONTEXT> simulator;
		private final WithDifferentStates.PostProcessor<STATE, CONTEXT> postProcessor;
		private final SortedMap<ZonedDateTime, Period<STATE, CONTEXT>> schedule = new TreeMap<>();

		private STATE[] availableStates;

		private WithDifferentStates(//
				STATE defaultState, //
				Supplier<STATE[]> availableStatesSupplier, //
				Function<GlobalSimulationsContext, CONTEXT> contextFunction, //
				Function<GlobalSimulationsContext, List<InitialPopulation<STATE>>> initialPopulationsFunction, //
				Simulator<STATE, CONTEXT> simulator, //
				WithDifferentStates.PostProcessor<STATE, CONTEXT> postProcessor) {
			super(contextFunction);
			this.defaultState = defaultState;
			this.availableStatesSupplier = availableStatesSupplier;
			this.initialPopulationsFunction = initialPopulationsFunction;
			this.simulator = simulator;
			this.postProcessor = postProcessor;
		}

		@Override
		public void initialize(GlobalSimulationsContext asc) {
			super.initialize(asc);
			this.availableStates = this.availableStatesSupplier.get();
		}

		public static record InitialPopulation<STATE>(List<ZonedDateTime> periods, STATE state) {

			/**
			 * Creates a {@link InitialPopulation} record.
			 * 
			 * @param <STATE> the type of the State
			 * @param periods a List of {@link ZonedDateTime}s
			 * @param state   the state
			 * @return a {@link InitialPopulation} record
			 */
			public static <STATE> InitialPopulation<STATE> of(List<ZonedDateTime> periods, STATE state) {
				return new InitialPopulation<STATE>(periods, state);
			}
		}

		/**
		 * Generates {@link InitialPopulation}s for this
		 * {@link EnergyScheduleHandler.WithDifferentStates}.
		 * 
		 * @param gsc the {@link GlobalSimulationsContext}
		 * @return a List of {@link InitialPopulation}s
		 */
		public List<InitialPopulation<STATE>> getInitialPopulations(GlobalSimulationsContext gsc) {
			return this.initialPopulationsFunction.apply(gsc);
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
		 * @return additional cost to be considered by the cost function
		 */
		public double simulatePeriod(OneSimulationContext osc, GlobalSimulationsContext.Period period,
				EnergyFlow.Model model, int stateIndex) {
			return this.simulator.simulate(osc, period, model, this.context, this.availableStates[stateIndex]);
		}

		/**
		 * Post-processes a Period of the best Schedule.
		 * 
		 * <p>
		 * This method is called internally after the Simulations are executed with the
		 * found best Schedule.
		 * 
		 * @param period     the {@link GlobalSimulationsContext.Period}
		 * @param osc        the {@link OneSimulationContext}
		 * @param energyFlow the {@link EnergyFlow}
		 * @param stateIndex the index of the simulated state
		 * @return the post-processed state index
		 */
		public int postProcessPeriod(GlobalSimulationsContext.Period period, OneSimulationContext osc,
				EnergyFlow energyFlow, int stateIndex) {
			return this.getStateIndex(
					this.postProcessor.postProcess(osc, energyFlow, this.context, this.availableStates[stateIndex]));
		}

		public static record Period<STATE, CONTEXT>(
				/** STATE of the Period */
				STATE state,
				/** Price [1/MWh] */
				double price, //
				/** EnergyScheduleHandler Context */
				CONTEXT context, //
				/** Simulated EnergyFlow */
				EnergyFlow energyFlow, //
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
				schedule.forEach((k, t) -> {
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

		/**
		 * Gets the stateIndex for the given STATE.
		 * 
		 * @param state the STATE
		 * @return the stateIndex; or zero if not found
		 */
		private int getStateIndex(STATE state) {
			var states = this.availableStates;
			for (var i = 0; i < states.length; i++) {
				if (states[i] == state) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public String toString() {
			var toStringHelper = toStringHelper("ESH.WithDifferentStates");
			var availableStates = this.availableStates;
			if (availableStates != null) {
				toStringHelper.add("availableStates", Arrays.toString(availableStates));
			}
			super.buildToString(toStringHelper);
			return toStringHelper.toString();
		}
	}

	public static final class WithOnlyOneState<CONTEXT> extends AbstractEnergyScheduleHandler<CONTEXT> {

		public static final class Builder<CONTEXT> {

			private Function<GlobalSimulationsContext, CONTEXT> contextFunction;
			private WithOnlyOneState.Simulator<CONTEXT> simulator;

			/**
			 * Sets a {@link Function} to create a {@link GlobalSimulationsContext}.
			 * 
			 * @param contextFunction the context function
			 * @return myself
			 */
			public Builder<CONTEXT> setContextFunction(Function<GlobalSimulationsContext, CONTEXT> contextFunction) {
				this.contextFunction = contextFunction;
				return this;
			}

			/**
			 * Sets a {@link WithDifferentStates.Simulator} that modifies a given
			 * {@link EnergyFlow}.
			 * 
			 * @param simulator a simulator
			 * @return myself
			 */
			public Builder<CONTEXT> setSimulator(WithOnlyOneState.Simulator<CONTEXT> simulator) {
				this.simulator = simulator;
				return this;
			}

			/**
			 * Builds the {@link EnergyScheduleHandler.WithDifferentStates} instance.
			 *
			 * @return a {@link EnergyScheduleHandler.WithDifferentStates}
			 */
			public WithOnlyOneState<CONTEXT> build() {
				return new EnergyScheduleHandler.WithOnlyOneState<CONTEXT>(this.contextFunction, this.simulator);
			}
		}

		/**
		 * Create a {@link EnergyScheduleHandler.WithOnlyOneState} for a
		 * {@link Controller} with only a single state.
		 *
		 * @param <CONTEXT> the type of the Context
		 * @return a {@link Builder}
		 */
		public static <CONTEXT> Builder<CONTEXT> create() {
			return new Builder<CONTEXT>();
		}

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
			var toStringHelper = toStringHelper("ESH.WithOnlyOneState");
			super.buildToString(toStringHelper);
			return toStringHelper.toString();
		}
	}
}
