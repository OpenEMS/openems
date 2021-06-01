package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.edge.batteryinverter.refu88k.RefuStore88k;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		RefuStore88k inverter = context.getParent();

		switch (inverter.getOperatingState()) {
		case STANDBY:
		case UNDEFINED:
			// Mark as stopped
			inverter._setStartStop(StartStop.STOP);
			return State.STOPPED;
		case FAULT:
			// Mark as stopped
			inverter._setStartStop(StartStop.STOP);
			return State.ERROR;
		case STARTING:
		case MPPT:
		case THROTTLED:
		case STARTED:
		case SHUTTING_DOWN:
		case OFF:
		case SLEEPING:
		
		}
		return State.UNDEFINED;
	}
}
