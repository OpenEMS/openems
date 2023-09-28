package io.openems.edge.battery.enfasbms.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.EnfasBms;
import io.openems.edge.battery.enfasbms.enums.CommandStateRequest;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.enfasbms.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		this.timeAtEntry = Instant.now();
		battery._setMaxStartTime(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {

		boolean isMaxAllowedStopTimePassed = Duration.between(this.timeAtEntry, Instant.now())
				.getSeconds() > Constants.MAX_ALLOWED_GO_STOP_TIME_SECONDS;

		System.out.println(Duration.between(this.timeAtEntry, Instant.now()).getSeconds());
		System.out.println(isMaxAllowedStopTimePassed);

		if (isMaxAllowedStopTimePassed) {
			return State.ERROR;
		}

		// TODO reduce the current to 1A or undefined, Then close the contactors
		// if Current <= 1A || Current == undefined , close the contactors
		// battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		var battery = context.getParent();

		if (this.checkCurrent(battery)) {
			battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		}

		// TODO do not add this in parallel cluster
		if (this.checkVoltageDrop(battery)) {
			return State.STOPPED;
		}
		// TODO add a error channel for drop voltage
		// TODO check the hv+ voltage decreasing from pack voltage
		var systemGlobalState = battery.getSystemGlobalState();

		return switch (systemGlobalState) {
		case BMS_CHARGE_STATE, BMS_DISCHARGE_STATE, BMS_IDLE_STATE, BMS_SLEEP_STATE, BMS_INITIALIZATION,
				BMS_OFFLINE_STATE, UNDEFINED ->
			State.GO_STOPPED;
		case BMS_ERROR_STATE, BMS_SAFE_STATE -> State.ERROR;

		};

	}

	private boolean checkVoltageDrop(EnfasBms battery) {

		if (!battery.getVoltage().isDefined() //
				|| !battery.getPackHighVoltagePlus().isDefined()) {
			return false;
		}

		if (battery.getVoltage().get() > (battery.getPackHighVoltagePlus().get() + 30)) {
			return true;
		}
		return false;

	}

	private boolean checkCurrent(EnfasBms battery) throws OpenemsNamedException {

		if (!battery.getCurrent().isDefined()) {
			return false;
		}

		if (battery.getCurrent().get() <= 1) {
			return true;
		}

		return false;

		// TODO check this later
		/*
		 * var contactorState = battery.getCommandStateRequest(); if (//
		 * (battery.getCurrent().get() <= 1 || !battery.getCurrent().isDefined()) // &&
		 * contactorState != CommandStateRequest.COMMAND_OPEN_CONTACTORS) // {
		 * battery.setCommandStateRequest(CommandStateRequest.COMMAND_OPEN_CONTACTORS);
		 * return State.GO_STOPPED; } else { return State.GO_STOPPED; }
		 */

	}
}