package io.openems.edge.controller.ess.standby.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //
		DISCHARGE(1), //
		SLOW_CHARGE_1(2), //
		FAST_CHARGE(3), //
		SLOW_CHARGE_2(4), //
		FINISHED(5) //
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
		case UNDEFINED:
			return new UndefinedHandler();
		case DISCHARGE:
			return new DischargeHandler();
		case SLOW_CHARGE_1:
			return new SlowCharge1Handler();
		case FAST_CHARGE:
			return new FastChargeHandler();
		case SLOW_CHARGE_2:
			return new SlowCharge2Handler();
		case FINISHED:
			return new FinishedHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}
}
