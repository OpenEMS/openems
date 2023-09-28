package io.openems.edge.battery.enfasbms.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		GO_RUNNING(10), //
		RUNNING(11), //

		GO_STOPPED(20), //
		STOPPED(21), //

		ERROR(30), //
		;

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
		default:
			break;
		case UNDEFINED:
			return new UndefinedHandler();
		case GO_RUNNING:
			return new GoRunningHandler();
		case RUNNING:
			return new RunningHandler();
		case GO_STOPPED:
			return new GoStoppedHandler();
		case STOPPED:
			return new StoppedHandler();
		case ERROR:
			return new ErrorHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}
}
