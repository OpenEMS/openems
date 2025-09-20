package io.openems.edge.controller.ess.standby.statemachine;

import java.time.LocalDate;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.standby.Config;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (!this.isActiveDay(context)) {
			return State.UNDEFINED;
		}

		// Start with discharge
		return State.DISCHARGE;
	}

	/**
	 * Checks if the logic should be run today.
	 * 
	 * @param context the {@link Context}
	 * @return true if today is within {@link Config#startDate()} and
	 *         {@link Config#endDate()} and is the configured
	 *         {@link Config#dayOfWeek()}
	 */
	private boolean isActiveDay(Context context) {
		var today = LocalDate.now(context.clock);
		if (today.isBefore(context.configuredStartDate)) {
			return false;
		}
		if (today.isAfter(context.configuredEndDate)) {
			return false;
		}
		if (!today.getDayOfWeek().equals(context.configuredDayOfWeek)) {
			return false;
		}
		return true;
	}

}
