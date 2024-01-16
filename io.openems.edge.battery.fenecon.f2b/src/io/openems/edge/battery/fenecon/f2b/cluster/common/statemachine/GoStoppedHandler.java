package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.cluster.common.Constants;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now(context.clock);
		context.getParent()._setMaxStartAttemptsFailed(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var cluster = context.getParent();
		// Has Faults -> error handling
		if (cluster.hasFaults()) {
			return State.ERROR;
		}

		var now = Instant.now(context.clock);
		if (Duration.between(this.timeAtEntry, now).getSeconds() > Constants.MAX_ALLOWED_STOP_TIME) {
			cluster._setTimeoutStopBatteries(true);
		}

		cluster.stopBatteries();

		// Check if all batteries are stopped
		if (cluster.areAllBatteriesStopped()) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
