package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	public static final int FIFTEEN_MINUTES_IN_SECONDS = 15 * 60; // 15 minutes in seconds

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (this.isPreActivationTime(context)) {
			return State.PRE_ACTIVATION_STATE;
		} else {
			return State.UNDEFINED;
		}
	}

	/**
	 * Checks if the current time, as adjusted by the component manager's clock, is
	 * within the pre-activation time window. pre-activation time window is 15
	 * minutes before the start time.
	 *
	 * @param context the context
	 * @return {@code true} if the current time is within the activation time
	 *         window, {@code false} otherwise.
	 */
	private boolean isPreActivationTime(Context context) {
		var currentDateTime = ZonedDateTime.now(context.clock);
		var currentEpochSecond = currentDateTime.toEpochSecond();
		if (currentEpochSecond >= context.startTimestamp - FIFTEEN_MINUTES_IN_SECONDS
				&& currentEpochSecond <= context.startTimestamp + context.duration) {
			return true;
		}
		return false;
	}
}
