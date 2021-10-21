package io.openems.edge.controller.ess.cycle.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	// TODO add States for waiting time
	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //
		COMPLETED_CYCLE(0), //
		START_CHARGE(1), //
		START_DISCHARGE(2), //
		CONTINUE_WITH_CHARGE(3), //
		CONTINUE_WITH_DISCHARGE(4), //
		FINAL_SOC(5), //
		FINISHED(6); //

		private final int value;

		private State(int value) {
			this.value = value;
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
		switch (state) {
		case START_CHARGE:
			return new StartChargeHandler();
		case START_DISCHARGE:
			return new StartDischargeHandler();
		case CONTINUE_WITH_CHARGE:
			return new ContinueWithChargeHandler();
		case CONTINUE_WITH_DISCHARGE:
			return new ContinueWithDischargeHandler();
		case COMPLETED_CYCLE:
			return new CompletedCycleHandler();
		case FINAL_SOC:
			return new FinalSocHandler();
		case FINISHED:
			return new FinishedHandler();
		case UNDEFINED:
			return new UndefinedHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}
}