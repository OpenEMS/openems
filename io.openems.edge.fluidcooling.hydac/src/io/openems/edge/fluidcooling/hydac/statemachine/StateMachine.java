package io.openems.edge.fluidcooling.hydac.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		WAIT_FOR_TARGET(0), //

		RUNNING(1), //

		STOPPED(2), //
		;

		private final int value;

		State(int value) {
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
			return WAIT_FOR_TARGET;
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
		return switch (state) {
		case WAIT_FOR_TARGET -> new WaitForTargetHandler();
		case RUNNING -> new RunningHandler();
		case STOPPED -> new StoppedHandler();
		};
	}

}
