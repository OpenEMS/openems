package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		inverter._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		// Has Faults -> abort
		if (inverter.hasFaults()) {
			return State.UNDEFINED;
		}

		return switch (inverter.getOperatingState()) {
		case STARTING //
			-> State.GO_RUNNING;
		case MPPT, STARTED, THROTTLED -> //
			// if inverter is throttled, full power is not available, but the device
			// is still working
			State.RUNNING;
		case STANDBY -> {
			inverter.exitStandbyMode();
			yield State.GO_RUNNING;
		}
		case FAULT //
			-> State.ERROR;
		case OFF, SLEEPING, SHUTTING_DOWN, UNDEFINED //
			-> State.UNDEFINED;
		};
	}

}
