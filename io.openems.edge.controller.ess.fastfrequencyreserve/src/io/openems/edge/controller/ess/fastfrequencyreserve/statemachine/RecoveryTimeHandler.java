package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;

public class RecoveryTimeHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (this.checkForFfrCycle(context.componentManager, context.startTimestamp, context.duration)) {
			return State.UNDEFINED;
		}
		return State.RECOVERY_TIME;

	}

	private boolean checkForFfrCycle(ComponentManager componentManager, long startTimestamp, int duration) {
		var now = ZonedDateTime.now(componentManager.getClock()).toEpochSecond();
		if (now >= startTimestamp + duration) {
			return true;
		}
		return false;
	}
}
