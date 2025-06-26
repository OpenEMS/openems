package io.openems.edge.ess.generic.symmetric.statemachine;

import com.google.common.base.CaseFormat;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		UNDEFINED(-1), //

		START_BATTERY(10), //
		START_BATTERY_INVERTER(11), //
		STARTED(12), //

		STOP_BATTERY_INVERTER(20), //
		STOP_BATTERY(21), //
		STOPPED(22), //

		ERROR(30), //
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
		return switch (state) {
		case UNDEFINED -> new UndefinedHandler();
		case START_BATTERY -> new StartBatteryHandler();
		case START_BATTERY_INVERTER -> new StartBatteryInverterHandler();
		case STARTED -> new StartedHandler();
		case STOP_BATTERY_INVERTER -> new StopBatteryInverterHandler();
		case STOP_BATTERY -> new StopBatteryHandler();
		case STOPPED -> new StoppedHandler();
		case ERROR -> new ErrorHandler();
		};
	}
}