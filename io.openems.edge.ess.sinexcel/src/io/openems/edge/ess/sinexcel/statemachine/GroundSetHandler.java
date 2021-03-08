package io.openems.edge.ess.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class GroundSetHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		// isOngrid ?
		if (!context.getGridDetector().get()) {
			// grounding set to goto ongrid
			context.component.handleWritingDigitalOutputForGrounding(false);
			context.component.handleWritingDigitalOutputForMain(false);
			return State.TOTAL_ONGRID;
		}

		// isOffgrid ?
		if (context.getGridDetector().get()) {
			// grounding set to goto ongrid
			context.component.handleWritingDigitalOutputForGrounding(true);
			return State.TOTAL_OFFGRID;
		}
		return State.GROUNDSET;
	}
}
