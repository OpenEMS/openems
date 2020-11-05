package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.component._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		// Has Faults -> abort
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		switch (context.component.getOperatingState()) {

		case STARTING:
			return State.GO_RUNNING;
		case MPPT:
		case STARTED:
		case THROTTLED:
			// if inverter is throttled, full power is not available, but the device
			// is still working
			return State.RUNNING;
		case STANDBY:
			context.component.exitStandbyMode();
			return State.GO_RUNNING;
			// if inverter is throttled, full power is not available, but the device
			// is still working
		case FAULT:
			return State.ERROR;
		case OFF:
		case SLEEPING:
		case SHUTTING_DOWN:
		
		case UNDEFINED:
			return State.UNDEFINED;
		}
		return State.UNDEFINED;
	}

}
