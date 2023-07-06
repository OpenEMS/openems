package io.openems.edge.controller.ess.cycle.statemachine;

import java.util.function.Supplier;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1, UndefinedHandler::new), //
		COMPLETED_CYCLE(0, CompletedCycleHandler::new), //
		START_CHARGE(1, StartChargeHandler::new), //
		START_DISCHARGE(2, StartDischargeHandler::new), //
		CONTINUE_WITH_CHARGE(3, ContinueWithChargeHandler::new), //
		CONTINUE_WITH_DISCHARGE(4, ContinueWithDischargeHandler::new), //
		WAIT_FOR_STATE_CHANGE(5, WaitForStateChangeHandler::new), //
		FINAL_SOC(6, FinalSocHandler::new), //
		FINISHED(7, FinishedHandler::new); //

		private final int value;
		private final Supplier<StateHandler<State, Context>> stateHandlerSupplier;

		private State(int value, Supplier<StateHandler<State, Context>> stateHandler) {
			this.value = value;
			this.stateHandlerSupplier = stateHandler;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}

		@Override
		public State[] getStates() {
			return State.values();
		}
	}

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		return state.stateHandlerSupplier.get();
	}
}