package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		switch (inverter.getCurrentState()) {
		case OFF:
		case STANDBY:
			// All Good
			return State.STOPPED;

		case GRID_CONNECTED:
		case THROTTLED:
		case FAULT:
		case GRID_PRE_CONNECTED:
		case MPPT:
		case NO_ERROR_PENDING:
		case PRECHARGE:
		case SHUTTING_DOWN:
		case SLEEPING:
		case STARTING:
		case UNDEFINED:
			// Not yet running...
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > BatteryInverterKacoBlueplanetGridsave.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return State.GO_STOPPED;
		}
		if (this.attemptCounter > BatteryInverterKacoBlueplanetGridsave.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			inverter._setMaxStopAttempts(true);
			return State.UNDEFINED;

		} else {
			// Trying to switch off
			inverter.setRequestedState(S64201RequestedState.OFF);
			this.lastAttempt = Instant.now();
			this.attemptCounter++;
			return State.GO_STOPPED;

		}
	}

}
