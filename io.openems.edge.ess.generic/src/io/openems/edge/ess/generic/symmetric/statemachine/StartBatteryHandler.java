package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StartBatteryHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(GenericManagedEss.TIMEOUT);

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;

		if (battery.isStarted()) {
			return State.START_BATTERY_INVERTER;
		}

		// Is max allowed start time passed ?
		if (this.timeout.elapsed(context.clock)) {
			ess._setTimeoutStartBattery(true);
			return State.ERROR;
		}

		battery.start();
		return State.START_BATTERY;
	}
}
