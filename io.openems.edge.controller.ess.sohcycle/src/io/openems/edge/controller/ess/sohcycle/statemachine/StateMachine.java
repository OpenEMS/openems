package io.openems.edge.controller.ess.sohcycle.statemachine;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;

public class StateMachine extends AbstractStateMachine<StateMachine.State, Context> {

	public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
		IDLE(1), //
		PREPARE(2), //
		REFERENCE_CYCLE_CHARGING(3), //
		REFERENCE_CYCLE_CHARGING_WAIT(4), //
		REFERENCE_CYCLE_DISCHARGING(5), //
		REFERENCE_CYCLE_DISCHARGING_WAIT(6), //
		MEASUREMENT_CYCLE_CHARGING(7), //
		MEASUREMENT_CYCLE_CHARGING_WAIT(8), //
		CHECK_BALANCING(9), //
		MEASUREMENT_CYCLE_DISCHARGING(10), //
		MEASUREMENT_CYCLE_DISCHARGING_WAIT(11), //
		EVALUATE_RESULT(12), //
		DONE(13), //
		ERROR_ABORT(14); //

		private final int value;

		State(int value) {
			this.value = value;
		}

		@Override
		public State[] getStates() {
			return State.values();
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
			return null;
		}
	}

	public StateMachine(State initialState) {
		super(initialState);
	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		return switch (state) {
		case IDLE -> new IdleHandler();
		case PREPARE -> new PrepareHandler();
		case REFERENCE_CYCLE_CHARGING -> new ReferenceCycleChargingHandler();
		case REFERENCE_CYCLE_CHARGING_WAIT -> new ReferenceCycleChargingWaitHandler();
		case REFERENCE_CYCLE_DISCHARGING -> new ReferenceCycleDischargingHandler();
		case REFERENCE_CYCLE_DISCHARGING_WAIT -> new ReferenceCycleDischargingWaitHandler();
		case MEASUREMENT_CYCLE_CHARGING -> new MeasurementCycleChargingHandler();
		case MEASUREMENT_CYCLE_CHARGING_WAIT -> new MeasurementCycleChargingWaitHandler();
		case CHECK_BALANCING -> new CheckBalancingHandler();
		case MEASUREMENT_CYCLE_DISCHARGING -> new MeasurementCycleDischargingHandler();
		case MEASUREMENT_CYCLE_DISCHARGING_WAIT -> new MeasurementCycleDischargingWaitHandler();
		case EVALUATE_RESULT -> new EvaluateResultHandler();
		case DONE -> new DoneHandler();
		case ERROR_ABORT -> new ErrorAbortHandler();
		};
	}
}
