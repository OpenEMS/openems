package io.openems.edge.battery.enfasbms.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.enums.GlobalState;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.enfasbms.utils.Constants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now();
		var battery = context.getParent();
		battery._setStartStop(StartStop.START);

	}

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		boolean isMaxAllowedRunningTimePassedInUndefinedState = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_START_TIME_SECONDS;

		if (battery.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		var systemGlobalState = battery.getSystemGlobalState();

		if (systemGlobalState == GlobalState.UNDEFINED && !isMaxAllowedRunningTimePassedInUndefinedState) {
			return State.RUNNING;
		}

		battery._setStartStop(StartStop.START);
		return switch (systemGlobalState) {
		case BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE -> State.RUNNING;
		case BMS_SLEEP_STATE, BMS_INITIALIZATION, BMS_OFFLINE_STATE, BMS_ERROR_STATE, BMS_SAFE_STATE, UNDEFINED ->
			State.ERROR;
		};
	}
}