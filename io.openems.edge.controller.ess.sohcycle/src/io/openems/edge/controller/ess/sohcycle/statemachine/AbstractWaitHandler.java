package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants.WAIT_DURATION_MINUTES;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;

public abstract class AbstractWaitHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(AbstractWaitHandler.class);

	protected abstract StateMachine.State getNextStateAfterWait();

	protected abstract StateMachine.State getCurrentState();

	protected Instant stateEnteredAt;

	@Override
	protected void onEntry(Context context) {
		this.stateEnteredAt = context.getClock().instant();
	}

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final Instant targetTime = this.stateEnteredAt.plus(WAIT_DURATION_MINUTES, ChronoUnit.MINUTES);
		final Instant now = context.getClock().instant();

		if (targetTime.isAfter(now)) {
			final long minutesLeft = ChronoUnit.MINUTES.between(now, targetTime);
			final int soc = context.ess.getSoc().orElse(0);
			context.logInfo(log, String.format(
					"%s: SoC=%d%%, waiting %d min, %d minutes left",
					this.getCurrentState().getName(), soc, WAIT_DURATION_MINUTES, minutesLeft));
			return this.getCurrentState();
		}

		return this.getNextStateAfterWait();
	}

	@Override
	protected void onExit(Context context) throws OpenemsError.OpenemsNamedException {
		super.onExit(context);
		this.stateEnteredAt = null;
	}
}
