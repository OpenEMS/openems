package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.victron.VictronBatteryInverterImpl;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final VictronBatteryInverterImpl inverter = context.getParent();
		switch (inverter.getStartStopTarget()) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return State.UNDEFINED;

		case START:
			// force START
			if (inverter.hasFaults()) {
				// Has Faults -> error handling
				return State.ERROR;
			} else {
				// No Faults -> start
				return State.GO_RUNNING;
			}

		case STOP:
			// force STOP
			return State.GO_STOPPED;
		}

		assert false;
		return State.UNDEFINED; // can never happen
	}

}
