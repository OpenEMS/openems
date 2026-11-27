package io.openems.edge.controller.ess.stepsoc.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;

public class AligningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var allowedCharge = context.ess.getAllowedChargePower();
		final var allowedDischarge = context.ess.getAllowedDischargePower();
		return switch (context.direction) {
		case CHARGE -> {
			context.ess.setActivePowerEqualsWithFilter(context.power);
			yield allowedDischarge.get() != 0 ? State.ALIGNING : State.RUNNING;
		}
		case DISCHARGE -> {
			context.ess.setActivePowerEqualsWithFilter(-context.power);
			yield allowedCharge.get() != 0 ? State.ALIGNING : State.RUNNING;
		}
		};
	}
}
