package io.openems.edge.ess.refu;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.ess.refu.RefuEss.ChannelId;

public class ErrorHandler implements Runnable {

	private enum State {
		GO_START(1), GO_ERROR_HANDLING(2), RESET_ERROR_ON(3), RESET_ERROR_OFF(4), RUNNING(5),
		WAIT_TILL_NEXT_ERROR_HANDLING(6);

		private int value;

		private State(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	private final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

	private final RefuEss parent;

	public ErrorHandler(RefuEss parent) {
		this.parent = parent;
	}

	private State currentSystemStateHandling = State.GO_START;
	private LocalDateTime errorOccurred = null;
	private LocalDateTime lastErrorReset = LocalDateTime.MIN;

	@Override
	public void run() {
		// Store current state of state machine
		IntegerReadChannel errorHandlerState = this.parent.channel(ChannelId.ERROR_HANDLER_STATE);
		errorHandlerState.setNextValue(this.currentSystemStateHandling.getValue());

		SystemState systemState = this.parent.channel(ChannelId.SYSTEM_STATE).value().asEnum();
		EnumWriteChannel setWorkStateChannel = this.parent.channel(ChannelId.SET_WORK_STATE);
		EnumWriteChannel systemErrorResetChannel = this.parent.channel(ChannelId.SET_SYSTEM_ERROR_RESET);

//		this.parent.logInfo(log,
//				"SystemState [" + systemState + "] StateHandling [" + this.currentSystemStateHandling
//						+ "] SetWorkState [" + setWorkStateChannel.value() + "] ErrorReset ["
//						+ systemErrorResetChannel.value() + "]");

		switch (this.currentSystemStateHandling) {
		case GO_START:
			/**
			 * Start the system, unless it is already running or has an error
			 */
			switch (systemState) {
			case STANDBY:
			case INIT:
			case OFF:
			case UNDEFINED:
			case PRE_OPERATION:
				try {
					setWorkStateChannel.setNextWriteValue(StopStart.START.getValue());
				} catch (OpenemsNamedException e) {
					this.parent.logError(this.log, "Unable to Set Work-State to START");
				}
				break;
			case OPERATION:
				this.currentSystemStateHandling = State.RUNNING;
				break;
			case ERROR:
				this.currentSystemStateHandling = State.GO_ERROR_HANDLING;
				break;
			}
			break;

		case RUNNING:
			/**
			 * System is running normally; otherwise start error handling
			 */
			switch (systemState) {
			case OPERATION:
				// do nothing
				break;
			case STANDBY:
			case INIT:
			case OFF:
			case UNDEFINED:
			case PRE_OPERATION:
			case ERROR:
				this.currentSystemStateHandling = State.GO_START;
			}
			break;

		case GO_ERROR_HANDLING:
			if (this.errorOccurred == null) {
				this.errorOccurred = LocalDateTime.now();
			}
			try {
				setWorkStateChannel.setNextWriteValue(StopStart.STOP.getValue());
			} catch (OpenemsNamedException e) {
				this.parent.logError(this.log, "Unable to Set Work-State to STOP");
			}
			if (this.lastErrorReset.isAfter(LocalDateTime.now().minusHours(2))) {
				// last reset more than 2 hours
				this.currentSystemStateHandling = State.WAIT_TILL_NEXT_ERROR_HANDLING;
			} else if (this.errorOccurred.isBefore(LocalDateTime.now().minusSeconds(30))) {
				// error handling since 30 seconds
				this.currentSystemStateHandling = State.RESET_ERROR_ON;
				this.errorOccurred = null;
			}
			break;

		case WAIT_TILL_NEXT_ERROR_HANDLING:
			if (this.lastErrorReset.isBefore(LocalDateTime.now().minusHours(2))) {
				this.currentSystemStateHandling = State.GO_ERROR_HANDLING;
			}
			break;

		case RESET_ERROR_ON:
			if (systemErrorResetChannel.value().orElse(StopStart.STOP.getValue()) == StopStart.START.getValue()) {
				this.currentSystemStateHandling = State.RESET_ERROR_OFF;
			} else {
				try {
					systemErrorResetChannel.setNextWriteValue(StopStart.START.getValue());
				} catch (OpenemsNamedException e) {
					this.parent.logError(this.log, "Unable to Set System-Error-Reset to START");
				}
			}
			break;

		case RESET_ERROR_OFF:
			if (systemErrorResetChannel.value().orElse(StopStart.START.getValue()) == StopStart.STOP.getValue()) {
				this.currentSystemStateHandling = State.GO_START;
				this.lastErrorReset = LocalDateTime.now();
			} else {
				try {
					systemErrorResetChannel.setNextWriteValue(StopStart.STOP.getValue());
				} catch (OpenemsNamedException e) {
					this.parent.logError(this.log, "Unable to Set System-Error-Reset to STOP");
				}
			}
			break;
		}
	}
}
