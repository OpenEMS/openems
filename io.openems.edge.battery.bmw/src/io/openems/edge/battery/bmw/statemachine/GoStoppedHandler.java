package io.openems.edge.battery.bmw.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private static final int TIMEOUT = 120;
	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		var isTimeout = Duration.between(this.timeAtEntry, //
				Instant.now(context.clock)).getSeconds() > TIMEOUT;
		battery._setTimeoutStopBattery(isTimeout);
		if (isTimeout) {
			return State.ERROR;
		}

		if (battery.isShutdown()) {
			return State.STOPPED;
		}

		battery.stopBattery();
		return State.GO_STOPPED;
	}
}
