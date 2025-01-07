package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {

		/**
		 * Start state for new StateMachine, never entered again.
		 */
		UNDEFINED(-1), //

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
		 * State if SoC is 1% under configured reserve SoC.
		 */
		FORCE_CHARGE_PV(5), //

		/**
		 * State if SoC is 2 % under configured reserve SoC.
		 */
		FORCE_CHARGE_GRID(6), //
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
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
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

	private State lastActiveState = State.UNDEFINED;

	@Override
	public void run(Context context) throws OpenemsNamedException {
		if (!this.getPreviousState().equals(this.getCurrentState())) {
			this.lastActiveState = this.getPreviousState();
		}
		context.setLastActiveState(this.lastActiveState);
		super.run(context);
	}

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<StateMachine.State, Context> getStateHandler(State state) {
		return switch (state) {
		case NO_LIMIT -> new NoLimitHandler();
		case ABOVE_RESERVE_SOC -> new AboveReserveSocHandler();
		case AT_RESERVE_SOC -> new AtReserveSocHandler();
		case BELOW_RESERVE_SOC -> new BelowReserveSocHandler();
		case FORCE_CHARGE_PV -> new ForceChargePvHandler();
		case FORCE_CHARGE_GRID -> new ForceChargeGridHandler();
		case UNDEFINED -> new UndefinedHandler();
		};
	}

}