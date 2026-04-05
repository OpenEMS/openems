package io.openems.edge.ruhfass.battery.batcon.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.batcon.enums.OperationModeBattery;
import io.openems.edge.ruhfass.battery.batcon.enums.RemainingBusSimulationStatus;
import io.openems.edge.ruhfass.battery.batcon.statemachine.StateMachine.State;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		// Mark as stopped
		battery._setStartStop(StartStop.STOP);
		battery.getStartStopChannel().nextProcessImage();
	}

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		if (battery.hasFaults()) {
			// Has Faults -> error handling
			return State.ERROR;
		}

		switch (battery.getStartStopTarget()) {
		case UNDEFINED:
		case STOP:
			break;
		case START:
			return State.GO_RUNNING;
		}

		if (battery.getRemainingBusSimulationStatus() == RemainingBusSimulationStatus.ACTIVE
				&& battery.getOperationMode() == OperationModeBattery.HV_INACTVE) {
			// Mark as stopped
			battery._setStartStop(StartStop.STOP);
			return State.STOPPED;
		}

		return State.ERROR;
	}

}
