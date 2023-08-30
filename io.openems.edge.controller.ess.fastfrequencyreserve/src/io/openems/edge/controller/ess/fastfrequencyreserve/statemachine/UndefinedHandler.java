package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	public static final int FIFTEEN_MINUTES_IN_SECONDS = 900;

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		if (this.isPreActivationTime(context.componentManager, context.startTimestamp, context.duration)) {
			return State.PRE_ACTIVATIOM_STATE;
		} else {
			return State.UNDEFINED;
		}
	}

	private boolean isPreActivationTime(ComponentManager cm, long startTimestamp, int duration) {

		var now = ZonedDateTime.now(cm.getClock())//
				.toEpochSecond();
		if (now >= startTimestamp - FIFTEEN_MINUTES_IN_SECONDS //
				&& now <= startTimestamp + duration) {
			return true;
		}
		return false;
	}
}
