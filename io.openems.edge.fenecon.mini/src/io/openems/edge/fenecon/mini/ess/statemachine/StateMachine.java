package io.openems.edge.fenecon.mini.ess.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		GO_READONLY_MODE(0), //
		ACTIVATE_ECONOMIC_MODE_1(1), //
		ACTIVATE_ECONOMIC_MODE_2(2), //
		ACTIVATE_ECONOMIC_MODE_3(3), //
		ACTIVATE_ECONOMIC_MODE_4(4), //
		READONLY_MODE(5), //

		GO_WRITE_MODE(10), //
		ACTIVATE_DEBUG_MODE_1(11), //
		ACTIVATE_DEBUG_MODE_2(12), //
		ACTIVATE_DEBUG_MODE_3(13), //
		ACTIVATE_DEBUG_MODE_4(14), //
		WRITE_MODE(19), //
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

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		switch (state) {
		case UNDEFINED:
			return new UndefinedHandler();

		case GO_READONLY_MODE:
			return new GoReadonlyModeHandler();
		case ACTIVATE_ECONOMIC_MODE_1:
			return new ActivateEconomicMode1Handler();
		case ACTIVATE_ECONOMIC_MODE_2:
			return new ActivateEconomicMode2Handler();
		case ACTIVATE_ECONOMIC_MODE_3:
			return new ActivateEconomicMode3Handler();
		case ACTIVATE_ECONOMIC_MODE_4:
			return new ActivateEconomicMode4Handler();
		case READONLY_MODE:
			return new ReadonlyModeHandler();

		case GO_WRITE_MODE:
			return new GoWriteModeHandler();
		case ACTIVATE_DEBUG_MODE_1:
			return new ActivateDebugMode1Handler();
		case ACTIVATE_DEBUG_MODE_2:
			return new ActivateDebugMode2Handler();
		case ACTIVATE_DEBUG_MODE_3:
			return new ActivateDebugMode3Handler();
		case ACTIVATE_DEBUG_MODE_4:
			return new ActivateDebugMode4Handler();
		case WRITE_MODE:
			return new WriteModeHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}
}
