package io.openems.edge.controller.ess.stepsoc.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (!context.ess.getSoc().isDefined()) {
			throw new OpenemsException("SoC is not defined - unable to run StepSoc state machine");
		}

		if (context.direction.isFinished(context.ess)) {
			return State.FINISHED;
		}

		if (this.isSocStepBoundary(context)) {
			context.getParent().setPausedAtStep(context.ess.getSoc().get());
			return State.STANDBY;
		}

		context.ess.setActivePowerEqualsWithoutFilter(context.power * context.direction.getFactor());
		return State.RUNNING;
	}

	private boolean isSocStepBoundary(Context context) {
		final var controller = context.getParent();
		final var soc = context.ess.getSoc();
		final int value = soc.get();

		if (value % context.socStep != 0) {
			return false;
		}

		if (value == controller.getPausedAtStep()) {
			return false;
		}

		return switch (context.direction) {
		case CHARGE -> value > 0;
		case DISCHARGE -> value < 100;
		};
	}
}