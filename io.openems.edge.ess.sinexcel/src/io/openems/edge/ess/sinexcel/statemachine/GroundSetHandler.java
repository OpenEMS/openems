package io.openems.edge.ess.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class GroundSetHandler extends StateHandler<State, Context> {
	
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.attemptCounter = 0;
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		
		
		// isOngrid ?
		if (!context.getGridDetector().get() ) {
			// grounding set to goto ongrid
			context.component.handleWritingDigitalOutputForGrounding(true);
			context.component.handleWritingDigitalOutputForMain(false);
			
			// main also here
			this.attemptCounter++;
			return State.TOTAL_ONGRID;
		}

		// grounding set to goto offgrid
		// isOffgrid ?
		if (context.getGridDetector().get() ) {
			// grounding set to goto ongrid
			context.component.handleWritingDigitalOutputForGrounding(false);
			this.attemptCounter++;
			return State.TOTAL_OFFGRID;
		}
		
		if (this.attemptCounter  > 5) {
			return State.ERROR;
		}

		return State.GROUNDSET;
	}

}
