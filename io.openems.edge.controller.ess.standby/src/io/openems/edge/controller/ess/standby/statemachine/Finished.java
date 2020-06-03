package io.openems.edge.controller.ess.standby.statemachine;

import java.time.LocalDate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;

public class Finished extends StateHandler<State, Context> {

	private LocalDate enteredStateAt = null;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.enteredStateAt = LocalDate.now(context.clock);
	}

	@Override
	public State getNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		// Apply power constraint
		context.ess.getSetActivePowerEqualsWithPid().setNextWriteValue(0);

		// TODO Stop ESS via StartStoppable

		// Evaluate next state
		if (LocalDate.now(context.clock).isAfter(this.enteredStateAt)) {
			// day changed
			return State.UNDEFINED;
		} else {
			// stay in this State
			return State.FINISHED;
		}
	}

}
