package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.batteryInverter.stop();
		context.battery.stop();
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var ess = context.getParent();
		final var battery = context.battery;
		final var batteryInverter = context.batteryInverter;
		// TODO error handling

		/*
		 * Wait at least for stopping the battery and check for ess, battery,
		 * battery-inverter faults
		 * 
		 * If ModbusCommunicationFault would be a FaultState, check it explicitly. The
		 * battery could still have a communication fault while starting the battery.
		 */
		if (!ess.hasFaults() && !batteryInverter.hasFaults() && !battery.hasFaults() && context.battery.isStopped()) {
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		ess._setTimeoutStartBattery(false);
		ess._setTimeoutStopBattery(false);
		ess._setTimeoutStartBatteryInverter(false);
		ess._setTimeoutStopBatteryInverter(false);
	}
}
