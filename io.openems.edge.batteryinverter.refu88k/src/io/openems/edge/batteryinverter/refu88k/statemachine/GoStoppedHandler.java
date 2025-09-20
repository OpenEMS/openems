package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		return switch (inverter.getOperatingState()) {
		case STARTING, MPPT, THROTTLED, STARTED -> {
			inverter.stopInverter();
			yield State.GO_STOPPED;
		}
		case FAULT, STANDBY //
			-> State.STOPPED;
		case SHUTTING_DOWN, OFF, SLEEPING, UNDEFINED //
			-> State.UNDEFINED;
		};
	}
}
