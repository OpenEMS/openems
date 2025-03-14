package io.openems.edge.energy.api.handler;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.function.Function;

import org.apache.logging.log4j.util.Supplier;

import io.openems.common.function.TriConsumer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

/**
 * Helper methods and classes for {@link EnergyScheduleHandler.WithOnlyOneMode}.
 */
public class OneMode {

	public static final class Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		private final String id;

		private Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction;
		private Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction;
		private Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parent the parent {@link OpenemsComponent}
		 */
		protected Builder(OpenemsComponent parent) {
			this(parent.serviceFactoryPid(), parent.id());
		}

		/**
		 * Sets the parent Factory-PID and Component-ID as unique ID for easier
		 * debugging.
		 * 
		 * @param parentFactoryPid the parent Factory-PID
		 * @param parentId         the parent ID
		 */
		protected Builder(String parentFactoryPid, String parentId) {
			this((parentFactoryPid == null || parentFactoryPid.isBlank() ? "" : (parentFactoryPid + ":")) //
					+ parentId);
		}

		/**
		 * Sets the unique identification ID.
		 * 
		 * @param id the ID
		 */
		public Builder(String id) {
			this.id = id;
		}

		/**
		 * Sets a {@link Function} to create a ControllerOptimizationContext from a
		 * {@link GlobalOptimizationContext}.
		 * 
		 * @param cocFunction the ControllerOptimizationContext function
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setOptimizationContext(
				Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction) {
			this.cocFunction = cocFunction;
			return this;
		}

		/**
		 * Sets a {@link Supplier} to create a ControllerOptimizationContext.
		 * 
		 * @param cocSupplier the ControllerOptimizationContext supplier
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setOptimizationContext(
				Supplier<OPTIMIZATION_CONTEXT> cocSupplier) {
			this.cocFunction = gsc -> cocSupplier.get();
			return this;
		}

		/**
		 * Sets a {@link Function} to create a ControllerScheduleContext.
		 * 
		 * @param cscFunction the ControllerScheduleContext function
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setScheduleContext(
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
		 * Sets a {@link Simulator} that simulates a Mode for one Period of a Schedule.
		 * 
		 * @param simulator a simulator
		 * @return myself
		 */
		public Builder<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> setSimulator(
				TriConsumer<GlobalScheduleContext, OPTIMIZATION_CONTEXT, EnergyFlow.Model> simulator) {
			this.simulator = (period, gsc, coc, csc, ef) -> simulator.accept(gsc, coc, ef);
			return this;
		}

		/**
		 * Builds the {@link EnergyScheduleHandler.WithOnlyOneMode} instance.
		 *
		 * @return a {@link EnergyScheduleHandler.WithOnlyOneMode}
		 */
		public EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> build() {
			return new EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT>(//
					this.id, //
					this.cocFunction == null //
							? goc -> null // fallback
							: this.cocFunction, //
					this.cscFunction == null //
							? coc -> null // fallback
							: this.cscFunction, //
					this.simulator == null //
							? (period, gsc, coc, csc, ef) -> doNothing() // fallback
							: this.simulator);
		}
	}

	public static interface Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> {

		/**
		 * Simulates one Period of a Schedule.
		 *
		 * @param period the {@link GlobalSimulationsContext.Period}
		 * @param gsc    the {@link GlobalScheduleContext}
		 * @param coc    the ControllerOptimizationContext
		 * @param csc    the ControllerScheduleContext
		 * @param ef     the {@link EnergyFlow.Model}
		 */
		public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc,
				OPTIMIZATION_CONTEXT coc, SCHEDULE_CONTEXT csc, EnergyFlow.Model ef);
	}

	private OneMode() {
	}
}
