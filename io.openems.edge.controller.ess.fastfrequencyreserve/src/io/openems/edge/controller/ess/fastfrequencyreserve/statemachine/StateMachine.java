package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;


//UNDEFINED(-1, "Undefined"),
//
//PRE_ACTIVATIOM_STATE(0, "Time before the Activation"),
//
//ACTIVATION_TIME(1, "Detected the freq dip, Start discharging"),
//
//SUPPORT_DURATION(3, "Hold discharging"),
//
//DEACTIVATION_TIME(4, "Set 0[W] power"),
//
//BUFFERED_SUPPORT(5, "Buffer support duration"),
//
//BUFFERED_TIME_BEFORE_RECOVERY(6, "Buffer time before recovery"),
//
//RECOVERY_TIME(7, "Recovery time")

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {

		UNDEFINED(-1),

		PRE_ACTIVATIOM_STATE(0),

		ACTIVATION_TIME(1),

		SUPPORT_DURATION(3),

		DEACTIVATION_TIME(4),

		BUFFERED_TIME_BEFORE_RECOVERY(5),		

		RECOVERY_TIME(7)
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
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
		
		@Override
		public String getName() {
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
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
		case UNDEFINED:
			return new UndefinedHandler();
		case PRE_ACTIVATIOM_STATE:
			return new PreActivationHandler();
		case ACTIVATION_TIME:
			return new ActivationTimeHandler();
		case SUPPORT_DURATION:
			return new SupportDurationTimeHandler();
		case DEACTIVATION_TIME:
			return new DeactivationTimeHandler();
		case BUFFERED_TIME_BEFORE_RECOVERY:
			return new BufferedTimeBeforeRecoveryHandler();
		case RECOVERY_TIME:
			return new RecoveryTimeHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}

}
