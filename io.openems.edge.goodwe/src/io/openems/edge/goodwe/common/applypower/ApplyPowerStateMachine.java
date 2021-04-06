package io.openems.edge.goodwe.common.applypower;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class ApplyPowerStateMachine extends AbstractStateMachine<ApplyPowerStateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		READ_ONLY(10), //

		/*
		 * GoodWe ET handling
		 */
		ET_EMPTY(20), //
		ET_DEFAULT(21), //
		ET_FULL(22), //

		/*
		 * GoodWe BT handling
		 */
		BT(30), //
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

	public ApplyPowerStateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		switch (state) {
		case UNDEFINED:
			return new UndefinedHandler();
		case READ_ONLY:
			return new ReadOnlyHandler();
		case ET_EMPTY:
			return new EtEmptyHandler();
		case ET_DEFAULT:
			return new EtDefaultHandler();
		case ET_FULL:
			return new EtFullHandler();
		case BT:
			return new BtHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

}
