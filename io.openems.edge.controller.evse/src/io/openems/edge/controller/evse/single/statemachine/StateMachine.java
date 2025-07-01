package io.openems.edge.controller.evse.single.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		CHARGING(10), //

		PHASE_SWITCH_TO_THREE_PHASE(20), //
		PHASE_SWITCH_TO_SINGLE_PHASE(21), //
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
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case CHARGING -> new ChargingHandler();
		case PHASE_SWITCH_TO_THREE_PHASE -> new PhaseSwitchHandler.ToThreePhase();
		case PHASE_SWITCH_TO_SINGLE_PHASE -> new PhaseSwitchHandler.ToSinglePhase();
		};
	}
}