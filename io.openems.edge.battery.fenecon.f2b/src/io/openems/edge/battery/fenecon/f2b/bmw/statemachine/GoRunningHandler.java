package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmw;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.ContactorDiagnosticStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.GoRunningSubState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.HvContactorStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.InsulationMeasurement;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bCanCommunication;
import io.openems.edge.battery.fenecon.f2b.common.enums.F2bState;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT = 120; // [s], per SubState
	private static final int TOGGLE_TIMEOUT = 10; // [s]

	private final Logger log = LoggerFactory.getLogger(GoRunningHandler.class);
	private int toggleCounter = 0;

	protected static record GoRunningState(GoRunningSubState subState, Instant lastChange) {
	}

	protected GoRunningState goRunningState;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.goRunningState = new GoRunningState(GoRunningSubState.ENABLE_CAN_COMMUNICATION,
				Instant.now(context.clock));
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		if (battery.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}
		if (battery.getF2bT30cNoInputVoltage().isDefined() && battery.getF2bT30cNoInputVoltage().get()) {
			battery._setEmergencyAcknowledge(true);
			return State.GO_RUNNING;
		}
		battery._setEmergencyAcknowledge(false);

		// Handle the Sub-StateMachine
		var nextSubState = this.getNextSubState(context);
		battery.channel(BatteryFeneconF2bBmw.ChannelId.GO_RUNNING_STATE_MACHINE).setNextValue(nextSubState);

		var now = Instant.now(context.clock);
		if (nextSubState != this.goRunningState.subState) {
			// Record State changes
			this.goRunningState = new GoRunningState(nextSubState, now);
		} else if (this.goRunningState.lastChange.isBefore(now.minusSeconds(TIMEOUT))) {
			// Handle GoRunningHandler State-timeout
			throw new OpenemsException("Timeout [" + TIMEOUT + "s] in GoRunning-" + this.goRunningState.subState);
		}

		if (nextSubState == GoRunningSubState.ERROR) {
			return State.ERROR;
		}

		if (nextSubState == GoRunningSubState.FINISHED) {
			return State.RUNNING;
		}

		return State.GO_RUNNING;
	}

	private GoRunningSubState getNextSubState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		return switch (this.goRunningState.subState) {
		case ENABLE_CAN_COMMUNICATION -> {
			if (!battery.getF2bTerminal30c().isDefined()) {
				context.logWarn(this.log, "F2B Terminal 30C is not defined!");
				yield GoRunningSubState.ENABLE_CAN_COMMUNICATION;
			}

			if (!battery.getF2bTerminal30c().get()) {
				battery.setF2bTerminal30c(true);
				yield GoRunningSubState.ENABLE_CAN_COMMUNICATION;
			}

			context.initializeBatteryCanSignals();
			battery.setF2bTerminal15Hw(true);
			if (!battery.getF2bTerminal15Hw().get()) {
				yield GoRunningSubState.ENABLE_CAN_COMMUNICATION;
			}

			battery.setF2bCanCommunication(F2bCanCommunication.CAN_ON);
			var f2bState = battery.getF2bState();
			if (f2bState == F2bState.CAN_ON) {
				yield GoRunningSubState.TOGGLE_TERMINAL_15_HW;
			}
			if (f2bState == F2bState.ERROR) {
				battery.setF2bReset(true);
				yield GoRunningSubState.ENABLE_CAN_COMMUNICATION;
			}
			battery.setF2bTerminal15ToggleRequest(true);
			yield GoRunningSubState.ENABLE_CAN_COMMUNICATION;
		}
		case TOGGLE_TERMINAL_15_HW -> {
			if (context.isAnyActiveCatError()) {
				var now = Instant.now(context.clock);
				if (this.goRunningState.lastChange.isAfter(now.minusSeconds(TOGGLE_TIMEOUT))) {
					throw new OpenemsException(
							"Timeout [" + TOGGLE_TIMEOUT + "s] in GoRunning-" + this.goRunningState.subState);
				}
			}
			yield GoRunningSubState.CLOSE_HV_CONTACTORS;
		}
		case CLOSE_HV_CONTACTORS -> {
			// Try toggle max 3 times
			if (this.toggleCounter < 3) {
				if (context.isAnyActiveCatError()) {
					battery.setF2bTerminal15ToggleRequest(true);
					this.toggleCounter++;
					yield GoRunningSubState.TOGGLE_TERMINAL_15_HW;
				}
			}

			// Awaiting for battery start unlock
			if (battery.isHvContactorUnlocked()) {
				battery.setHvContactor(true);
			} else {
				yield GoRunningSubState.CLOSE_HV_CONTACTORS;
			}

			if (battery.getContactorsDiagnosticStatus() == ContactorDiagnosticStatus.ONE_CONTACTOR_STUCK
					|| battery.getContactorsDiagnosticStatus() == ContactorDiagnosticStatus.TWO_CONTACTOR_STUCK) {
				battery._setHvContactorsStuckFailed(true);
				yield GoRunningSubState.ERROR;
			}

			if (battery.getHvContactorStatus().asEnum() == HvContactorStatus.CONTACTORS_CLOSED) {
				battery.setInsulationMeasurement(InsulationMeasurement.PERFORM_MEASUREMENT);
				yield GoRunningSubState.FINISHED;
			}
			yield GoRunningSubState.CLOSE_HV_CONTACTORS;
		}
		case FINISHED -> GoRunningSubState.FINISHED;
		case ERROR, UNDEFINED -> GoRunningSubState.ERROR;
		};
	}

	@Override
	protected String debugLog() {
		return State.GO_RUNNING.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.goRunningState.subState);
	}
}