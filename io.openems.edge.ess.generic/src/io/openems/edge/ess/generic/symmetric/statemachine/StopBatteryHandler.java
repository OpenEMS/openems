package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StopBatteryHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(GenericManagedEss.TIMEOUT);

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;

		if (context.hasFaults()) {
			return State.ERROR;
		}

		if (battery.isStopped()) {
			return State.STOPPED;
		}

		// Is max allowed start time passed ?
		if (this.timeout.elapsed(context.clock)) {
			ess._setTimeoutStopBattery(true);
			return State.ERROR;
		}

		battery.stop();
		return State.STOP_BATTERY;
	}
}
