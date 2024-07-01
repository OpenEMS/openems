package io.openems.edge.controller.ess.standby.statemachine;

import java.time.LocalDate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;

public class FinishedHandler extends StateHandler<State, Context> {

	private LocalDate enteredStateAt = null;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.enteredStateAt = LocalDate.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		// Apply power constraint
		context.ess.setActivePowerEqualsWithPid(0);

		// TODO Stop ESS via StartStoppable

		// Evaluate next state
		if (LocalDate.now(context.clock).isAfter(this.enteredStateAt)) {
			// day changed
			return State.UNDEFINED;
		}
		// stay in this State
		return State.FINISHED;
	}

}
