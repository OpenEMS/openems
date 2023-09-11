package io.openems.edge.battery.enfasbms.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.EnfasBms;
import io.openems.edge.battery.enfasbms.enums.CommandStateRequest;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.enfasbms.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now();
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		var battery = context.getParent();
		var systemGlobalState = battery.getSystemGlobalState();

		boolean isMaxTimePassedInError = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_TIME_SECONDS_IN_ERROR_BEFORE_OPENING_CONTACTORS;

		if (this.checkCurrent(battery)) {
			// Do not open contactor right away
			battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		}
		if (isMaxTimePassedInError) {
			battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		}

		return switch (systemGlobalState) {
		case BMS_ERROR_STATE, BMS_SAFE_STATE, BMS_SLEEP_STATE, UNDEFINED, BMS_INITIALIZATION, BMS_OFFLINE_STATE,
				BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE ->
			State.ERROR;
		};
	}

	private boolean checkCurrent(EnfasBms battery) throws OpenemsNamedException {

		return battery.getCurrent().isDefined() && battery.getCurrent().get() <= 1;

		// TODO check this later for serial/parallel connections
		/*
		 * var contactorState = battery.getCommandStateRequest(); if (//
		 * (battery.getCurrent().get() <= 1 || !battery.getCurrent().isDefined()) // &&
		 * contactorState != CommandStateRequest.COMMAND_OPEN_CONTACTORS) // {
		 * battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		 * return State.GO_STOPPED; } else { return State.GO_STOPPED; }
		 */
	}
}