package io.openems.edge.battery.enfasbms.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.enums.GlobalState;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.enfasbms.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now();
	}

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		boolean isMaxAllowedUndefinedTimePassed = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_UNDEFINED_TIME_SECONDS;

		if (isMaxAllowedUndefinedTimePassed) {
			return State.ERROR;
		}

		GlobalState systemGlobalState = battery.getSystemGlobalState();
		return switch (systemGlobalState) {
		case BMS_OFFLINE_STATE -> State.STOPPED;
		case BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE -> State.RUNNING;
		case BMS_ERROR_STATE, BMS_SAFE_STATE -> State.ERROR;
		case BMS_SLEEP_STATE, UNDEFINED, BMS_INITIALIZATION -> State.GO_STOPPED;
		};
	}
}