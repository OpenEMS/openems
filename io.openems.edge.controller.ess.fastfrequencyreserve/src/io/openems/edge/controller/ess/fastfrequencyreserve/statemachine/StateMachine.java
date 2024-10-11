package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //
		PRE_ACTIVATION_STATE(10), //
		ACTIVATION_TIME(20), //
		SUPPORT_DURATION(30), //
		DEACTIVATION_TIME(40), //
		BUFFERED_TIME_BEFORE_RECOVERY(50), //
		RECOVERY_TIME(60);//

		private final int value;

		private State(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}

		@Override
		public String getName() {
			return this.name();
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
		return switch (state) {
		case ACTIVATION_TIME -> new ActivationTimeHandler();
		case BUFFERED_TIME_BEFORE_RECOVERY -> new BufferedTimeBeforeRecoveryHandler();
		case DEACTIVATION_TIME -> new DeactivationTimeHandler();
		case PRE_ACTIVATION_STATE -> new PreActivationHandler();
		case RECOVERY_TIME -> new RecoveryTimeHandler();
		case SUPPORT_DURATION -> new SupportDurationTimeHandler();
		case UNDEFINED -> new UndefinedHandler();
		};
	}
}