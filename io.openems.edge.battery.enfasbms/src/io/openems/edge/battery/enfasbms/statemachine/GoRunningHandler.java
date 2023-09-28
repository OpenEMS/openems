package io.openems.edge.battery.enfasbms.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.enums.CommandStateRequest;
import io.openems.edge.battery.enfasbms.enums.GlobalState;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.enfasbms.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now();
		var battery = context.getParent();
		battery.setCommandStateRequest(CommandStateRequest.COMMAND_CLOSE_CONTACTORS);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		boolean isMaxAllowedGorunningTimePassed = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_START_TIME_SECONDS;

		if (isMaxAllowedGorunningTimePassed) {
			return State.ERROR;
		}

		battery.setCommandStateRequest(CommandStateRequest.COMMAND_CLOSE_CONTACTORS);
		GlobalState systemGlobalState = battery.getSystemGlobalState();

		return switch (systemGlobalState) {
		case BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE -> State.RUNNING;
		case BMS_ERROR_STATE, BMS_SAFE_STATE -> State.ERROR;
		case BMS_SLEEP_STATE, UNDEFINED, BMS_INITIALIZATION, BMS_OFFLINE_STATE -> State.GO_RUNNING;
		};
	}
}