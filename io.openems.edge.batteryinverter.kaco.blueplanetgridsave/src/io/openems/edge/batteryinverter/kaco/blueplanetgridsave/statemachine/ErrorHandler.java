package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private static final int WAIT_SECONDS = 120;

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();
		switch (inverter.getCurrentState()) {
		case STANDBY:
		case GRID_CONNECTED:
		case GRID_PRE_CONNECTED:
		case THROTTLED:
		case PRECHARGE:
		case MPPT:
		case STARTING:
		case OFF:
		case SHUTTING_DOWN:
		case SLEEPING:
			// no more error pending
			return State.UNDEFINED;
		case UNDEFINED:
			// TODO
			break;
		case FAULT:
		case NO_ERROR_PENDING:
			/*
			 * According to Manual: to more errors to be acknowledged - try to turn OFF
			 */
			// TODO this should not be set all the time
			inverter.setRequestedState(S64201RequestedState.OFF);
			break;
		}

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > WAIT_SECONDS) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
