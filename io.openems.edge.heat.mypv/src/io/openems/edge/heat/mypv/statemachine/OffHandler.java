package io.openems.edge.heat.mypv.statemachine;

import static io.openems.edge.heat.mypv.statemachine.MyPvConstants.OFF_ACTIVE_POWER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.mypv.statemachine.StateMachine.State;

public class OffHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
		return State.OFF;
	}
}
