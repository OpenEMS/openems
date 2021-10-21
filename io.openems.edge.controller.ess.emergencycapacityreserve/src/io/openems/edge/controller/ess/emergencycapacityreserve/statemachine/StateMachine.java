package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {

		/**
		 * State if SoC is greater then configured reserve SoC.
		 */
		NO_LIMIT(1), //

		/**
		 * State if SoC is 1% above configured reserve SoC.
		 */
		ABOVE_RESERVE_SOC(2), //

		/**
		 * State if SoC equals to the configured reserve SoC.
		 */
		AT_RESERVE_SOC(3), //

		/**
		 * State if SoC is under configured reserve SoC.
		 */
		BELOW_RESERVE_SOC(4), //

		/**
		 * State if SoC is 4% under configured reserve SoC.
		 */
		FORCE_CHARGE(5);

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
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
		}

		@Override
		public OptionsEnum getUndefined() {
			return NO_LIMIT;
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
	public StateHandler<StateMachine.State, Context> getStateHandler(State state) {
		switch (state) {
		case NO_LIMIT:
			return new NoLimitHandler();
		case ABOVE_RESERVE_SOC:
			return new AboveReserveSocHandler();
		case AT_RESERVE_SOC:
			return new AtReserveSocHandler();
		case BELOW_RESERVE_SOC:
			return new BelowReserveSocHandler();
		case FORCE_CHARGE:
			return new ForceChargeHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

}