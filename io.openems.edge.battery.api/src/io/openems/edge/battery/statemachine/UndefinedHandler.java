package io.openems.edge.battery.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now(context.componentManager.getClock());
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		var stateMachine = battery.getStateMachine();
		if (!stateMachine.isDefined()) {
			// wait some time until channel value defined
			if (Duration.between(this.entryAt, Instant.now(context.componentManager.getClock()))
					.getSeconds() > context.config.batteryStartStopTime()) {
				return State.ERROR;
			}
		}
		
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.isStarted()) {
			return State.RUNNING;
		}

		if (battery.isStopped()) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
