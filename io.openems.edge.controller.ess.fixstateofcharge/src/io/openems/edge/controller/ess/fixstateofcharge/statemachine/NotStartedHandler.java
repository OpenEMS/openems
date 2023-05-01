package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class NotStartedHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		var socState = Context.getSocState(context.soc, context.targetSoc);

		/*
		 * Switch state if no target time specified or target time already passed
		 */
		if (!context.considerTargetTime() || context.passedTargetTime()) {
			return socState;
		}

		/*
		 * Stay in "NotStarted" if there is still enough time left until the target
		 * time.
		 */
		var capacity = context.getParent().getEss().getCapacity().orElse(8_800);
		var power = Math.round(Math.min(context.maxApparentPower * AbstractFixStateOfCharge.DEFAULT_POWER_FACTOR,
				capacity * (1f / 3f)));

		var requiredSeconds = AbstractFixStateOfCharge.calculateRequiredTime(context.soc, context.targetSoc, capacity,
				power, context.clock);

		var startTime = context.getTargetTime().minus(requiredSeconds + context.config.getTargetTimeBuffer() * 60,
				ChronoUnit.SECONDS);

		// Start time not reached
		var t = ZonedDateTime.now(context.clock);
		if (startTime.isAfter(t)) {

			context.getParent()._setExpectedStartEpochSeconds(startTime.toEpochSecond());
			return State.NOT_STARTED;
		}

		// Start charging/discharging the system
		context.getParent()._setExpectedStartEpochSeconds(null);
		return socState;
	}
}
