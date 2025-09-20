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
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case DISCHARGE -> new DischargeHandler();
		case SLOW_CHARGE_1 -> new SlowCharge1Handler();
		case FAST_CHARGE -> new FastChargeHandler();
		case SLOW_CHARGE_2 -> new SlowCharge2Handler();
		case FINISHED -> new FinishedHandler();
		};
	}
}
