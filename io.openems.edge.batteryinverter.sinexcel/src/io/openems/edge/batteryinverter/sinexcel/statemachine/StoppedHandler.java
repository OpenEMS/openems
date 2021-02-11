package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Mark as stopped
		Sinexcel inverter = context.getParent();
		inverter._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}

}
