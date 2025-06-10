package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StopBatteryInverterHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(GenericManagedEss.TIMEOUT);

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();

		if (context.hasEssFaults()) {
			return State.ERROR;
		}

		if (context.batteryInverter.isStopped()) {
			return State.STOP_BATTERY;
		}

		// Is max allowed start time passed ?
		if (this.timeout.elapsed(context.clock)) {
			ess._setTimeoutStopBatteryInverter(true);
			return State.ERROR;
		}

		context.batteryInverter.stop();
		return State.STOP_BATTERY_INVERTER;
	}
}
