package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class RecoveryTimeHandler extends StateHandler<State, Context> {

	public static final int RECOVERY_DURATION_SECONDS = 15 * 60;

	@Override
	protected State runAndGetNextState(Context context) {
		if (this.isItWithinDuration(context)) {
			return State.ACTIVATION_TIME;
		}
		return State.RECOVERY_TIME;
	}

	private boolean isItWithinDuration(Context context) {
		var now = Instant.now(context.clock).getEpochSecond();
		var expiration = Duration//
				.between(context.getCycleStart(), ZonedDateTime.now(context.clock))//
				.toSeconds();
		return expiration > RECOVERY_DURATION_SECONDS || now >= context.startTimestamp + context.duration;
	}
}