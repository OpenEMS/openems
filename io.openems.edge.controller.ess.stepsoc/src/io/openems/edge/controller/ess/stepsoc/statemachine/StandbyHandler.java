package io.openems.edge.controller.ess.stepsoc.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;

public class StandbyHandler extends StateHandler<State, Context> {

	private Timeout timeout;

	@Override
	protected void onEntry(Context context) {
		this.timeout = Timeout.ofMinutes(context.standbyTime);
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		context.ess.setActivePowerEqualsWithoutFilter(0);
		return this.timeout.elapsed(context.clock) ? State.RUNNING : State.STANDBY;
	}
}
