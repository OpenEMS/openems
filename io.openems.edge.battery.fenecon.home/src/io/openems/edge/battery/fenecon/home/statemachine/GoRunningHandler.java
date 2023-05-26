package io.openems.edge.battery.fenecon.home.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static enum BatteryRelayState {
		WAIT_FOR_SWITCH_ON, WAIT_FOR_BMS_CONTROL, WAIT_FOR_SWITCH_OFF, FINISHED;
	}

	private final Logger log = LoggerFactory.getLogger(GoRunningHandler.class);

	private BatteryRelayState state = BatteryRelayState.WAIT_FOR_SWITCH_ON;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.state = BatteryRelayState.WAIT_FOR_SWITCH_ON;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		switch (this.state) {
		case WAIT_FOR_SWITCH_ON -> {
			if (context.isBatteryStarted()) {
				this.state = BatteryRelayState.WAIT_FOR_SWITCH_OFF;

			} else if (this.getBatteryStartUpRelay(context) == Boolean.TRUE) {
				// Relay switched on -> wait till battery is switched on
				this.state = BatteryRelayState.WAIT_FOR_BMS_CONTROL;

			} else {
				// Turns the 12 V power on for battery start-up
				this.setBatteryStartUpRelay(context, true);
			}
			
		}
		case WAIT_FOR_BMS_CONTROL -> {
			// Battery internal relay has not switched on yet, wait ...
			// TODO add timeout and throw error (see MaxStartAttempts in Soltaro as example)
			if (context.isBatteryStarted()) {
				this.state = BatteryRelayState.WAIT_FOR_SWITCH_OFF;
			} else {
				this.state = BatteryRelayState.WAIT_FOR_BMS_CONTROL;
			}
		}
		case WAIT_FOR_SWITCH_OFF -> {
			// Turns the 12 V power off after battery start-up
			this.setBatteryStartUpRelay(context, false);

			if (this.getBatteryStartUpRelay(context) != Boolean.TRUE) {
				// Relay switched off or Relay state unknown -> battery is running
				this.state = BatteryRelayState.FINISHED;
			}
		}
		case FINISHED -> {
			return State.RUNNING;
		 }
		}

		return State.GO_RUNNING;
	}

	private Boolean getBatteryStartUpRelay(Context context) {
		if (context.batteryStartUpRelayChannel != null) {
			return context.batteryStartUpRelayChannel.value().get();
		}
		return false;
	}

	/**
	 * Set Switch to OFF or Switch ON Operation.
	 *
	 * @param context the {@link Context}
	 * @param value   true to switch the relay on; <br/>
	 *                false to switch the relay off
	 * @throws OpenemsNamedException on error
	 */
	public void setBatteryStartUpRelay(Context context, boolean value) throws OpenemsNamedException {
		if (value) {
			if (context.batteryStartUpRelayChannel == null) {
				context.logInfo(this.log,
						"Because of the wrong/missed configured Battery Start Up Relay Channel Address, relay CAN NOT SWITCH ON.");
				return;

			} else {
				context.logInfo(this.log,
						"Set output [" + context.batteryStartUpRelayChannel.address() + "] SWITCHED ON.");
			}
		} else {
			if (context.batteryStartUpRelayChannel == null) {
				context.logInfo(this.log,
						"Because of the wrong/missed configured Battery Start Up Relay Channel Address, relay CAN NOT SWITCH OFF.");
				return;

			} else {
				context.logInfo(this.log,
						"Set output [" + context.batteryStartUpRelayChannel.address() + "] SWITCHED OFF.");
			}
		}
		context.batteryStartUpRelayChannel.setNextWriteValue(value);
	}

}
