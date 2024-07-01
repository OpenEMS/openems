package io.openems.edge.battery.fenecon.home.statemachine;

import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT = 120; // [s], per SubState
	private static final int INTERNAL_TIMEOUT = 60; // [s], must be less than TIMEOUT

	protected static enum SubState {
		UNDEFINED, //
		INITIAL_WAIT_FOR_BMS_CONTROL, //
		START_UP_RELAY_ON, //
		START_UP_RELAY_HOLD, //
		START_UP_RELAY_OFF, //
		RETRY_MODBUS_COMMUNICATION, //
		WAIT_FOR_BMS_CONTROL, //
		WAIT_FOR_MODBUS_COMMUNICATION, //
		FINISHED;
	}

	protected static record GoRunningState(SubState subState, Instant lastChange) {
	}

	protected GoRunningState grs;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.grs = new GoRunningState(SubState.UNDEFINED, Instant.now(context.clock));
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		// Handle the Sub-StateMachine
		var nextSubState = this.getNextSubState(context);

		var now = Instant.now(context.clock);
		if (nextSubState != this.grs.subState) {
			// Record State changes
			this.grs = new GoRunningState(nextSubState, now);
		} else if (this.grs.lastChange.isBefore(now.minusSeconds(TIMEOUT))) {
			// Handle GoRunningHandler State-timeout
			throw new OpenemsException("Timeout [" + TIMEOUT + "s] in GoRunning-" + this.grs.subState);
		}

		if (nextSubState == SubState.FINISHED) {
			// Sub-StateMachine finished
			return State.RUNNING;
		}

		return State.GO_RUNNING;
	}

	private SubState getNextSubState(Context context) throws OpenemsNamedException {
		return switch (this.grs.subState) {
		case UNDEFINED -> {
			if (context.bmsControl == null) {
				// Modbus Communication might not be established yet
				yield SubState.INITIAL_WAIT_FOR_BMS_CONTROL;

			} else if (context.bmsControl) {
				// Battery is already started -> make sure StartUpRelay is turned off
				yield SubState.START_UP_RELAY_OFF;

			} else {
				// Battery is communicating, but stopped -> try to start
				yield SubState.START_UP_RELAY_ON;
			}
		}

		case INITIAL_WAIT_FOR_BMS_CONTROL -> {
			if (context.bmsControl == Boolean.TRUE) {
				// Waited successfully, Battery is started
				// -> make sure StartUpRelay is turned off
				yield SubState.START_UP_RELAY_OFF;

			} else if (context.bmsControl == Boolean.FALSE) {
				// Battery is communicating, but stopped -> try to start
				yield SubState.START_UP_RELAY_ON;
			}

			// BMS_CONTROL is undefined -> apply internal timeout for Modbus Communication
			var now = Instant.now(context.clock);
			if (this.grs.lastChange.isBefore(now.minusSeconds(INTERNAL_TIMEOUT))) {
				// Timeout -> try to start
				yield SubState.START_UP_RELAY_ON;
			}
			// Keep waiting
			yield SubState.INITIAL_WAIT_FOR_BMS_CONTROL;
		}

		case START_UP_RELAY_ON -> {
			if (context.batteryStartUpRelay == Boolean.TRUE) {
				// Successfully switched StartUpRelay ON -> toggle
				yield SubState.START_UP_RELAY_HOLD;
			}

			// Switch StartUpRelay ON
			context.setBatteryStartUpRelay.accept(true);

			// Apply internal Timeout
			var now = Instant.now(context.clock);
			if (this.grs.lastChange.isBefore(now.minusSeconds(INTERNAL_TIMEOUT))) {
				// Timeout; ignore StartUpRelay state
				yield SubState.RETRY_MODBUS_COMMUNICATION;
			}
			// Keep waiting
			yield SubState.START_UP_RELAY_ON;
		}

		case START_UP_RELAY_HOLD -> {
			// Wait 10s
			var now = Instant.now(context.clock);
			if (this.grs.lastChange.isBefore(now.minusSeconds(10))) {
				// Finished waiting
				yield SubState.START_UP_RELAY_OFF;
			}
			// Keep waiting
			yield SubState.START_UP_RELAY_HOLD;
		}

		case START_UP_RELAY_OFF -> {
			// Switch StartUpRelay ON
			context.setBatteryStartUpRelay.accept(false);

			if (context.batteryStartUpRelay != Boolean.TRUE) {
				// Successfully switched StartUpRelay OFF or lost connection
				yield SubState.RETRY_MODBUS_COMMUNICATION;
			}
			// Keep waiting
			yield SubState.START_UP_RELAY_OFF;
		}

		case RETRY_MODBUS_COMMUNICATION -> {
			context.retryModbusCommunication.run();
			yield SubState.WAIT_FOR_BMS_CONTROL;
		}

		case WAIT_FOR_BMS_CONTROL -> {
			if (context.bmsControl == Boolean.TRUE) {
				yield SubState.WAIT_FOR_MODBUS_COMMUNICATION;
			}
			// Keep waiting
			yield SubState.WAIT_FOR_BMS_CONTROL;
		}

		case WAIT_FOR_MODBUS_COMMUNICATION -> {
			if (context.modbusCommunicationFailed) {
				yield SubState.WAIT_FOR_MODBUS_COMMUNICATION;
			} else {
				yield SubState.FINISHED;
			}
		}

		case FINISHED ->
			// Finished.
			SubState.FINISHED;
		};
	}

	@Override
	protected String debugLog() {
		return State.GO_RUNNING.asCamelCase() + "-" + EnumUtils.nameAsCamelCase(this.grs.subState);
	}

}
