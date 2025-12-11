package io.openems.edge.energy.api.handler;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

/**
 * Helper methods and classes for
 * {@link EnergyScheduleHandler.WithDifferentModes}.
 */
public class DifferentModes {

	public static final class Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> extends
			AbstractEnergyScheduleHandler.Builder<Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		private BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, Modes<MODE>> modesFunction;
		private InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider = InitialPopulationsProvider
				.empty();
		private Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator = Simulator.doNothing();
		private PreProcessor<MODE, OPTIMIZATION_CONTEXT> preProcessor = PreProcessor.doNothing();
		private PostProcessor<MODE, OPTIMIZATION_CONTEXT> postProcessor = PostProcessor.doNothing();

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parent the parent {@link OpenemsComponent}
		 */
		protected Builder(OpenemsComponent parent) {
			super(parent);
		}

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parentFactoryPid the parent Factory-PID
		 * @param parentId         the parent ID
		 */
		public Builder(String parentFactoryPid, String parentId) {
			super(parentFactoryPid, parentId);
		}

		protected Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> self() {
			return this;
		}

		/**
		 * Sets a {@link Supplier} for {@link Modes}.
		 * 
		 * <p>
		 * First given Mode is used as default or fallback.
		 * 
		 * @param supplier a {@link Modes} supplier
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setModes(Supplier<Modes<MODE>> supplier) {
			this.modesFunction = (goc, context) -> supplier.get();
			return this;
		}

		/**
		 * Sets a {@link Function} for {@link Modes}.
		 * 
		 * <p>
		 * First given Mode is used as default or fallback.
		 * 
		 * @param function a {@link Modes} function
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setModes(
				BiFunction<GlobalOptimizationContext, OPTIMIZATION_CONTEXT, Modes<MODE>> function) {
			this.modesFunction = function;
			return this;
		}

		/**
		 * Sets a {@link InitialPopulationsProvider} to provide
		 * {@link InitialPopulation}s.
		 * 
		 * @param initialPopulationsProvider the {@link InitialPopulationsProvider}
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setInitialPopulationsProvider(
				InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> initialPopulationsProvider) {
			this.initialPopulationsProvider = initialPopulationsProvider;
			return this;
		}

		/**
		 * Sets a {@link PreProcessor} to exchange a Mode of a Period before Simulation,
		 * i.e. replace with fixed or manually planned Mode.
		 * 
		 * @param preProcessor the {@link PreProcessor}
		 * @return myself
		 */
		public Builder<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setPreProcessor(
				PreProcessor<MODE, OPTIMIZATION_CONTEXT> preProcessor) {
			this.preProcessor = preProcessor;
			return this;
		}

		/**
		 * Sets a {@link Simulator} that simulates a Mode for one Period of a Schedule.
		 * 
		 * @param simulator a {@link Simulator}
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
					this.parentFactoryPid, this.parentId, this.serializer, //
					this.modesFunction, //
					this.cocFunction, //
					this.cscFunction, //
					this.initialPopulationsProvider, //
					this.preProcessor, //
					this.simulator, //
					this.postProcessor);
		}
	}

	public static class Modes<MODE> {
		public static record Mode<MODE>(MODE mode, boolean addToOptimizer) {
			@Override
			public final String toString() {
				return new StringBuilder(this.mode.toString()) //
						.append("[").append(this.addToOptimizer ? "+" : "-").append("]") //
						.toString();
			}
		}

		/**
		 * Create empty {@link Modes}.
		 * 
		 * @param <MODE> the type of the Mode
		 * @return empty {@link Modes}
		 */
		public static <MODE> Modes<MODE> empty() {
			return new Modes<MODE>(ImmutableList.of());
		}

		/**
		 * Create {@link Modes} from an Array of MODEs, add all to Optimizer.
		 * 
		 * @param <MODE> the type of the Mode
		 * @param modes  the MODEs array
		 * @return the {@link Modes}
		 */
		public static <MODE> Modes<MODE> of(MODE[] modes) {
			return new Modes<MODE>(Arrays.stream(modes) //
					.map(m -> new Mode<MODE>(m, true)) //
					.collect(toImmutableList()));
		}

		/**
		 * Create {@link Modes} from an ImmutableList of {@link Mode}s.
		 * 
		 * @param <MODE> the type of the Mode
		 * @param modes  the {@link Mode}s
		 * @return the {@link Modes}
		 */
		public static <MODE> Modes<MODE> of(ImmutableList<Mode<MODE>> modes) {
			return new Modes<MODE>(modes);
		}

		private final ImmutableList<Mode<MODE>> allModes;
		private final ImmutableList<Mode<MODE>> optimizerModes;

		private Modes(ImmutableList<Mode<MODE>> modes) {
			this.allModes = modes;
			this.optimizerModes = this.allModes.stream() //
					.filter(Mode::addToOptimizer) //
					.collect(toImmutableList());
		}

		/**
		 * Are any MODEs available?.
		 * 
		 * @return boolean
		 */
		public boolean isEmpty() {
			return this.allModes.isEmpty();
		}

		/**
		 * Are any MODEs with the `addToOptimizer` property available?.
		 * 
		 * @return boolean
		 */
		public boolean hasForOptimizer() {
			return this.optimizerModes.isEmpty();
		}

		/**
		 * Creates a {@link Stream} with all {@link Mode}s.
		 * 
		 * @return the {@link Stream}
		 */
		public Stream<Mode<MODE>> streamAll() {
			return this.allModes.stream();
		}

		/**
		 * Creates an {@link IntStream} with indices of all {@link Mode}s.
		 * 
		 * @return the {@link IntStream}
		 */
		public IntStream streamAllIndices() {
			return IntStream.range(0, this.allModes.size());
		}

		/**
		 * Creates a {@link Stream} of MODEs.
		 * 
		 * @return the {@link Stream}
		 */
		public Stream<MODE> streamForOptimizer() {
			return this.optimizerModes.stream().map(Mode::mode);
		}

		/**
		 * Gets the `addToOptimizer` property for the given index.
		 * 
		 * @param index the index
		 * @return the property; or false
		 */
		public boolean addToOptimizer(int index) {
			return this._get(index) //
					.map(Mode::addToOptimizer) //
					.orElse(false);
		}

		/**
		 * Gets the MODE for the given index.
		 * 
		 * @param index the index
		 * @return the given MODE; first MODE as fallback; null if modes is empty or
		 *         index is negative
		 */
		public MODE get(int index) {
			return this._get(index) //
					.map(Mode::mode) //
					.orElse(null);
		}

		/**
		 * Gets a String representation of the MODE for the given index.
		 * 
		 * @param index the index
		 * @return the String; fallback to "UNDEFINED"
		 */
		public String getAsString(int index) {
			var mode = this.get(index);
			return mode == null //
					? "UNDEFINED" //
					: mode.toString();
		}

		private Optional<Mode<MODE>> _get(int index) {
			if (this.allModes.isEmpty() || index < 0) {
				return Optional.empty();
			} else if (index < this.allModes.size()) {
				return Optional.of(this.allModes.get(index));
			} else {
				return Optional.of(this.allModes.getFirst());
			}
		}

		/**
		 * Gets the index for the given MODE.
		 * 
		 * @param mode the MODE
		 * @return the index; or zero if not found
		 */
		public int getIndex(MODE mode) {
			for (var i = 0; i < this.allModes.size(); i++) {
				if (this.allModes.get(i).mode.equals(mode)) {
					return i;
				}
			}
			return 0;
		}

		@Override
		public final String toString() {
			var t = toStringHelper("Modes");
			this.allModes.stream() //
					.forEach(m -> t.addValue(m.toString()));
			return t.toString();
		}
	}

	public static record InitialPopulation<MODE>(MODE[] modes) implements Comparable<InitialPopulation<MODE>> {

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
		 * @param modes  the MODEs
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
					.addValue(Arrays.toString(this.modes)) //
					.toString();
		}

		@Override
		public int compareTo(InitialPopulation<MODE> o) {
			return this.toString().compareTo(o.toString());
		}
	}

	public static record Period<MODE, OPTIMIZATION_CONTEXT>(
			/** Duration of the Period */
			PeriodDuration duration,
			/** MODE of the Period */
			MODE mode,
			/** Price [1/MWh] */
			double price, //
			/** ControllerOptimizationContext */
			OPTIMIZATION_CONTEXT coc, //
			/** Simulated EnergyFlow */
			EnergyFlow energyFlow, //
			/** the initial ESS energy in the beginning of the period in [Wh] */
			int essInitialEnergy) implements EnergyScheduleHandler.Period<OPTIMIZATION_CONTEXT> {

		/**
		 * This class is only used internally to apply the Schedule.
		 */
		public static record Transition(PeriodDuration duration, int modeIndex, double price, EnergyFlow energyFlow,
				int essInitialEnergy) {
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
			return new Period<>(t.duration, getMode.apply(t.modeIndex), t.price, coc, t.energyFlow, t.essInitialEnergy);
		}
	}

	public static interface InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> {

		/**
		 * Provide no initial population.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param <SCHEDULE_CONTEXT>     the type of the ControllerScheduleContext
		 * @return empty provider
		 */
		public static <MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> InitialPopulationsProvider<MODE, OPTIMIZATION_CONTEXT> empty() {
			return (goc, coc, modes) -> ImmutableSortedSet.of();
		}

		/**
		 * Provides initial populations.
		 *
		 * @param goc   the {@link GlobalOptimizationContext}
		 * @param coc   the ControllerOptimizationContext
		 * @param modes the {@link Modes}
		 * @return a list of {@link InitialPopulation}s
		 */
		public ImmutableSortedSet<InitialPopulation<MODE>> get(GlobalOptimizationContext goc, OPTIMIZATION_CONTEXT coc,
				Modes<MODE> modes);
	}

	public static interface PreProcessor<MODE, OPTIMIZATION_CONTEXT> {

		/**
		 * A 'do-nothing' {@link PreProcessor}.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @return the same Mode
		 */
		public static <MODE, OPTIMIZATION_CONTEXT> PreProcessor<MODE, OPTIMIZATION_CONTEXT> doNothing() {
			return (period, context, state) -> state;
		}

		/**
		 * Pre-Process a Mode of a Period before Simulation, i.e. replace with fixed or
		 * manually planned Mode.
		 * 
		 * @param period the {@link GlobalOptimizationContext.Period}
		 * @param coc    the ControllerOptimizationContext
		 * @param mode   the initial Mode
		 * @return the new Mode
		 */
		public MODE preProcess(GlobalOptimizationContext.Period period, OPTIMIZATION_CONTEXT coc, MODE mode);
	}

	public static interface Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		/**
		 * A 'do-nothing' {@link Simulator}.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param <SCHEDULE_CONTEXT>     the type of the ControllerScheduleContext
		 * @return zero cost
		 */
		public static <MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> Simulator<MODE, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> doNothing() {
			return (id, period, gsc, coc, csc, ef, mode, fitness) -> doNothing();
		}

		/**
		 * Simulates a Mode for one Period of a Schedule.
		 *
		 * @param parentComponentId the parent Component-ID
		 * @param period            the {@link GlobalOptimizationContext.Period}
		 * @param gsc               the {@link GlobalScheduleContext}
		 * @param coc               the ControllerOptimizationContext
		 * @param csc               the ControllerScheduleContext
		 * @param ef                the {@link EnergyFlow.Model}
		 * @param mode              the simulated Mode
		 * @param fitness           the {@link Fitness} result
		 */
		public void simulate(String parentComponentId, GlobalOptimizationContext.Period period,
				GlobalScheduleContext gsc, OPTIMIZATION_CONTEXT coc, SCHEDULE_CONTEXT csc, EnergyFlow.Model ef,
				MODE mode, Fitness fitness);
	}

	public static interface PostProcessor<MODE, OPTIMIZATION_CONTEXT> {

		/**
		 * A 'do-nothing' {@link PostProcessor}.
		 * 
		 * @param <MODE>                 the type of the Mode
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @return the same Mode
		 */
		public static <MODE, OPTIMIZATION_CONTEXT> PostProcessor<MODE, OPTIMIZATION_CONTEXT> doNothing() {
			return (id, period, osc, energyFlow, context, state) -> state;
		}

		/**
		 * Post-Process a Mode of a Period during Simulation, i.e. replace with 'better'
		 * Mode with the equivalent behaviour.
		 * 
		 * <p>
		 * NOTE: heavy computation is ok here, because this method is called only at the
		 * end with the best Schedule.
		 * 
		 * @param parentComponentId the parent Component-ID
		 * @param period            the {@link GlobalOptimizationContext.Period}
		 * @param gsc               the {@link GlobalScheduleContext}
		 * @param ef                the {@link EnergyFlow}
		 * @param coc               the ControllerOptimizationContext
		 * @param mode              the initial Mode
		 * @return the new Mode
		 */
		public MODE postProcess(String parentComponentId, GlobalOptimizationContext.Period period,
				GlobalScheduleContext gsc, EnergyFlow ef, OPTIMIZATION_CONTEXT coc, MODE mode);
	}

	private DifferentModes() {
	}
}
