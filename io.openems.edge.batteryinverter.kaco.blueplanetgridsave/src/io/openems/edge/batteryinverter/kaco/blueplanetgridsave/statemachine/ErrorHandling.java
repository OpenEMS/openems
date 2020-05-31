package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201_RequestedState;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandling extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();
	}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		switch (context.component.getCurrentState()) {
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
		case FAULT:
		case UNDEFINED:
			// TODO
			break;
		case NO_ERROR_PENDING:
			/*
			 * According to Manual: to more errors to be acknowledged - try to turn OFF
			 */
			// TODO this should not be set all the time
			context.component.setRequestedState(S64201_RequestedState.OFF);
			break;
		}

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR_HANDLING;
	}

}
