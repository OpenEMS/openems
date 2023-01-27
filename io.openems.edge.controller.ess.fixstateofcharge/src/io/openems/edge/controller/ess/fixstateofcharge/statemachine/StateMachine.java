package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {

		/**
		 * Controller is active, but not running (Property isRunning still false).
		 */
		IDLE(1), //

		/**
		 * State if the system is still waiting (LeftTime > ReqiredTime + Buffer).
		 */
		NOT_STARTED(2), //

		/**
		 * State if SoC is above configured target SoC.
		 */
		ABOVE_TARGET_SOC(3), //

		/**
		 * State if SoC is below configured target SoC.
		 */
		BELOW_TARGET_SOC(4), //

		/**
		 * State if SoC equals to the configured target SoC.
		 */
		AT_TARGET_SOC(5),

		/**
		 * State if SoC is below configured target SoC but within boundaries.
		 */
		WITHIN_LOWER_TARGET_SOC_BOUNDARIES(6), //

		/**
		 * State if SoC is above configured target SoC but within boundaries.
		 */
		WITHIN_UPPER_TARGET_SOC_BOUNDARIES(7);

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
			return IDLE;
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
		case IDLE:
			return new IdleHander();
		case NOT_STARTED:
			return new NotStartedHandler();
		case ABOVE_TARGET_SOC:
			return new AboveTargetSocHandler();
		case BELOW_TARGET_SOC:
			return new BelowTargetSocHandler();
		case AT_TARGET_SOC:
			return new AtTargetSocHandler();
		case WITHIN_LOWER_TARGET_SOC_BOUNDARIES:
			return new WithinLowerTargetSocBoundariesHandler();
		case WITHIN_UPPER_TARGET_SOC_BOUNDARIES:
			return new WithinUpperTargetSocBoundariesHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

}