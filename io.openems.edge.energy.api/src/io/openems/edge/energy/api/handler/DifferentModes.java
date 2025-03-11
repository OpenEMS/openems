package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

/**
 * Helper methods and classes for
 * {@link EnergyScheduleHandler.WithDifferentModes}.
 */
public class DifferentModes {

	public static final class Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		private String componentId;
		private MODE defaultMode;
		private BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, MODE[]> availableModesFunction;
		private Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction;
		private Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction;
		private BiFunction<GlobalOptimizationContext, MODE[], ImmutableList<InitialPopulation<MODE>>> initialPopulationsFunction;
		private Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;
		private PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor = PostProcessor.doNothing();

		/**
		 * Sets the parent Component-ID for easier debugging.
		 * 
		 * @param componentId the parent Component-ID
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setComponentId(String componentId) {
			this.componentId = componentId;
			return this;
		}

		/**
		 * Sets the default Mode if no other is explicitly scheduled.
		 * 
		 * @param mode a Mode
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setDefaultMode(MODE mode) {
			this.defaultMode = mode;
			return this;
		}

		/**
		 * Sets a {@link Function} to create a ControllerOptimizationContext.
		 * 
		 * @param cocFunction the ControllerOptimizationContext function
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setOptimizationContext(
				Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction) {
			this.cocFunction = cocFunction;
			return this;
		}

		/**
		 * Sets a {@link Function} to create a ControllerScheduleContext.
		 * 
		 * @param cscFunction the ControllerScheduleContext function
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setScheduleContext(
				Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction) {
			this.cscFunction = cscFunction;
			return this;
		}

		/**
		 * Sets a {@link Supplier} to create a ControllerScheduleContext.
		 * 
		 * @param cscSupplier the ControllerScheduleContext supplier
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setScheduleContext(
				Supplier<SCHEDULE_CONTEXT> cscSupplier) {
			this.cscFunction = coc -> cscSupplier.get();
			return this;
		}

		/**
		 * Sets a {@link Supplier} for available Modes.
		 * 
		 * @param supplier a Modes supplier
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setAvailableModes(Supplier<MODE[]> supplier) {
			this.availableModesFunction = (goc, context) -> supplier.get();
			return this;
		}

		/**
		 * Sets a {@link Function} for available Modes.
		 * 
		 * @param function a Modes function
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setAvailableModes(
				BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, MODE[]> function) {
			this.availableModesFunction = function;
			return this;
		}

		/**
		 * Sets a {@link Function} to provide {@link InitialPopulation}s.
		 * 
		 * @param initialPopulationsFunction the function
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setInitialPopulationsFunction(
				BiFunction<GlobalOptimizationContext, MODE[], ImmutableList<InitialPopulation<MODE>>> initialPopulationsFunction) {
			this.initialPopulationsFunction = initialPopulationsFunction;
			return this;
		}

		/**
		 * Sets a {@link Simulator} that simulates a Mode for one Period of a Schedule.
		 * 
		 * @param simulator a simulator
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setSimulator(
				Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator) {
			this.simulator = simulator;
			return this;
		}

		/**
		 * Sets a {@link PostProcessor}.
		 * 
		 * @param postProcessor a {@link PostProcessor}
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setPostProcessor(
				PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor) {
			this.postProcessor = postProcessor;
			return this;
		}

		/**
		 * Builds an instance of {@link EnergyScheduleHandler.EshWithDifferentModes}.
		 *
		 * @return a {@link EnergyScheduleHandler.EshWithDifferentModes}
		 */
		public EshWithDifferentModes<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> build() {
			return new EshWithDifferentModes<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>(//
					this.componentId == null //
							? "ESH.WithDifferentModes." + Integer.toHexString(this.hashCode()) // fallback
							: this.componentId, //
					this.defaultMode, this.availableModesFunction, //
					this.cocFunction == null //
							? goc -> null // fallback
							: this.cocFunction, //
					this.cscFunction == null //
							? coc -> null // fallback
							: this.cscFunction, //
					this.initialPopulationsFunction == null //
							? (goc, availableModes) -> ImmutableList.of() // fallback
							: this.initialPopulationsFunction, //
					this.simulator == null //
							? (period, gsc, coc, csc, ef, mode) -> 0. // fallback
							: this.simulator, //
					this.postProcessor);
		}
	}

	public static record InitialPopulation<MODE>(MODE[] modes) {

		/**
		 * This class is only used internally to apply the InitialPopulation.
		 */
		public static record Transition(int[] modeIndexes) implements Comparable<Transition> {
			@Override
			public final String toString() {
				return toStringHelper("InitialPopulation") //
						.addValue(Arrays.toString(this.modeIndexes)) //
						.toString();
			}

			@Override
			public int compareTo(Transition o) {
				return Arrays.compare(this.modeIndexes, o.modeIndexes);
			}
		}

		/**
		 * Creates a {@link InitialPopulation} record.
		 * 
		 * @param <MODE> the type of the Mode
		 * @param modes  the Modes
		 * @return a {@link InitialPopulation} record
		 */
		public static <MODE> InitialPopulation<MODE> of(MODE[] modes) {
			return new InitialPopulation<MODE>(modes);
		}

		protected Transition toTansition(ToIntFunction<MODE> toModeIndex) {
			return new Transition(stream(this.modes) //
					.mapToInt(m -> toModeIndex.applyAsInt(m)) //
					.toArray());
		}

		@Override
		public final String toString() {
			return toStringHelper("InitialPopulation") //
					.addValue(stream(this.modes) //
							.map(Object::toString) //
							.collect(joining(","))) //
					.toString();
		}
	}

	public static record Period<MODE, OPTIMIZATION_CONTEXT>(
			/** MODE of the Period */
			MODE mode,
			/** Price [1/MWh] */
			double price, //
			/** ControllerOptimizationContext */
			OPTIMIZATION_CONTEXT coc, //
			/** Simulated EnergyFlow */
			EnergyFlow energyFlow, //
			/** the initial ESS energy in the beginning of the period in [Wh] */
			int essInitialEnergy) {

		/**
		 * This class is only used internally to apply the Schedule.
		 */
		public static record Transition(int modeIndex, double price, EnergyFlow energyFlow, int essInitialEnergy) {
		}

		/**
		 * Builds a {@link EnergyScheduleHandler.WithDifferentStates.Period} from a
		 * {@link EnergyScheduleHandler.WithDifferentStates.Period.Transition} record.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param t                      the
		 *                               {@link EnergyScheduleHandler.WithDifferentStates.Period.Transition}
		 *                               record
		 * @param getMode                a method to translate a 'modeIndex' to a MODE
		 * @param coc                    the ControllerOptimizationContext used during
		 *                               simulation
		 * @return a {@link Period} record
		 */
		public static <MODE, OPTIMIZATION_CONTEXT> Period<MODE, OPTIMIZATION_CONTEXT> fromTransitionRecord(
				Period.Transition t, IntFunction<MODE> getMode, OPTIMIZATION_CONTEXT coc) {
			return new Period<>(getMode.apply(t.modeIndex), t.price, coc, t.energyFlow, t.essInitialEnergy);
		}
	}

	public static interface Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		/**
		 * Simulates a Mode for one Period of a Schedule.
		 *
		 * @param period the {@link GlobalOptimizationContext.Period}
		 * @param gsc    the {@link GlobalScheduleContext}
		 * @param coc    the ControllerOptimizationContext
		 * @param csc    the ControllerScheduleContext
		 * @param ef     the {@link EnergyFlow.Model}
		 * @param mode   the simulated Mode
		 * @return additional cost to be considered by the cost function
		 */
		public double simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc,
				OPTIMIZATION_CONTEXT coc, SCHEDULE_CONTEXT csc, EnergyFlow.Model ef, MODE mode);
	}

	public static interface PostProcessor<MODE, OPTIMIZATION_CONTEXT> {

		/**
		 * A 'do-nothing' {@link PostProcessor}.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @return the same State
		 */
		public static <MODE, OPTIMIZATION_CONTEXT> PostProcessor<MODE, OPTIMIZATION_CONTEXT> doNothing() {
			return (osc, energyFlow, context, state) -> state;
		}

		/**
		 * Post-Process a Mode of a Period during Simulation, i.e. replace with 'better'
		 * state with the equivalent behaviour.
		 * 
		 * <p>
		 * NOTE: heavy computation is ok here, because this method is called only at the
		 * end with the best Schedule.
		 * 
		 * @param gsc  the {@link GlobalScheduleContext}
		 * @param ef   the {@link EnergyFlow}
		 * @param coc  the ControllerOptimizationContext
		 * @param mode the initial Mode
		 * @return the new Mode
		 */
		public MODE postProcess(GlobalScheduleContext gsc, EnergyFlow ef, OPTIMIZATION_CONTEXT coc, MODE mode);
	}

	private DifferentModes() {
	}
}
