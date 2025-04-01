package io.openems.edge.energy.api.handler;

import static io.openems.common.utils.FunctionUtils.doNothing;

import org.apache.logging.log4j.util.Supplier;

import io.openems.common.function.TriConsumer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

/**
 * Helper methods and classes for {@link EnergyScheduleHandler.WithOnlyOneMode}.
 */
public class OneMode {

	public static final class Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> extends
			AbstractEnergyScheduleHandler.Builder<Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>, OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		private Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator = (id, period, gsc, coc, csc, ef,
				fitness) -> doNothing();

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

		protected Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> self() {
			return this;
		}

		/**
		 * Sets a {@link Supplier} to create a ControllerScheduleContext.
		 * 
		 * @param cscSupplier the ControllerScheduleContext supplier
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setScheduleContext(
				Supplier<SCHEDULE_CONTEXT> cscSupplier) {
			this.cscFunction = gsc -> cscSupplier.get();
			return this;
		}

		/**
		 * Sets a {@link Simulator} that simulates a Mode for one Period of a Schedule.
		 * 
		 * @param simulator a simulator
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setSimulator(
				Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator) {
			this.simulator = simulator;
			return this;
		}

		/**
		 * Sets a a simplified Simulator that simulates a Mode for one Period of a
		 * Schedule.
		 * 
		 * @param simulator a simulator
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setSimulator(
				TriConsumer<GlobalScheduleContext, OPTIMIZATION_CONTEXT, EnergyFlow.Model> simulator) {
			this.simulator = (id, period, gsc, coc, csc, ef, fitness) -> simulator.accept(gsc, coc, ef);
			return this;
		}

		/**
		 * Builds the {@link EnergyScheduleHandler.WithOnlyOneMode} instance.
		 *
		 * @return a {@link EnergyScheduleHandler.WithOnlyOneMode}
		 */
		public EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> build() {
			return new EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>(//
					this.parentFactoryPid, this.parentId, this.serializer, //
					this.cocFunction, //
					this.cscFunction, //
					this.simulator);
		}
	}

	public static record Period<OPTIMIZATION_CONTEXT>(
			/** Price [1/MWh] */
			double price, //
			/** ControllerOptimizationContext */
			OPTIMIZATION_CONTEXT coc, //
			/** Simulated EnergyFlow */
			EnergyFlow energyFlow) implements EnergyScheduleHandler.Period<OPTIMIZATION_CONTEXT> {

		/**
		 * This class is only used internally to apply the Schedule.
		 */
		public static record Transition(double price, EnergyFlow energyFlow) {
		}

		/**
		 * Builds a {@link EnergyScheduleHandler.OneMode.Period} from a
		 * {@link EnergyScheduleHandler.OneMode.Period.Transition} record.
		 * 
		 * @param <OPTIMIZATION_CONTEXT> the type of the ControllerOptimizationContext
		 * @param t                      the
		 *                               {@link EnergyScheduleHandler.WithDifferentStates.Period.Transition}
		 *                               record
		 * @param coc                    the ControllerOptimizationContext used during
		 *                               simulation
		 * @return a {@link Period} record
		 */
		public static <OPTIMIZATION_CONTEXT> Period<OPTIMIZATION_CONTEXT> fromTransitionRecord(Period.Transition t,
				OPTIMIZATION_CONTEXT coc) {
			return new Period<>(t.price, coc, t.energyFlow);
		}
	}

	public static interface Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		/**
		 * Simulates one Period of a Schedule.
		 *
		 * @param parentComponentId the parent Component-ID
		 * @param period            the {@link GlobalSimulationsContext.Period}
		 * @param gsc               the {@link GlobalScheduleContext}
		 * @param coc               the ControllerOptimizationContext
		 * @param csc               the ControllerScheduleContext
		 * @param ef                the {@link EnergyFlow.Model}
		 * @param fitness           the {@link Fitness} result
		 */
		public void simulate(String parentComponentId, GlobalOptimizationContext.Period period,
				GlobalScheduleContext gsc, OPTIMIZATION_CONTEXT coc, SCHEDULE_CONTEXT csc, EnergyFlow.Model ef,
				Fitness fitness);
	}

	private OneMode() {
	}
}
