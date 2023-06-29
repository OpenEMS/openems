package io.openems.edge.battery.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now(context.componentManager.getClock());
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		if (Duration.between(this.entryAt, Instant.now(context.componentManager.getClock()))
				.getSeconds() > context.config.batteryStartStopTime()) {
			if (battery.getMainContactorUnlocked()) {
				return State.RUNNING;
			}
		}
		return State.GO_RUNNING;
	}
}
