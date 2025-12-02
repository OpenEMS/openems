package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

    @Override
    public State runAndGetNextState(Context context) throws OpenemsNamedException {
	// final VictronBatteryInverterImpl inverter = context.getParent();

	// Mark as stopped
	// inverter._setStartStop(StartStop.STOP);
	return State.STOPPED;
    }

}
