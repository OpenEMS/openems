package io.openems.edge.ess.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.sinexcel.CurrentState;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

//import java.time.Duration;
//import java.time.Instant;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class TotalOnGridHandler extends StateHandler<State, Context> {

//	private final Logger log = LoggerFactory.getLogger(TotalOnGridHandler.class);

	protected boolean StartOnce = false;
//	private Instant lastAttempt = Instant.MIN;

//	@Override
//	protected void onEntry(Context context) throws OpenemsNamedException {
//		this.lastAttempt = Instant.MIN;		
//	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		//log.info("Inside  total ongrid handler");

		// If Grid is off
		if (context.getGridDetector().get() ) {

			// Before going to ON, Wait for some time, so that the grid is not fluctuating
			//boolean iswaitingTimePassed = Duration.between(this.lastAttempt, Instant.now()).getSeconds() > 65;
			//if (iswaitingTimePassed) {
				return State.STOP;
			//}
		}

		if (!StartOnce) {
			this.StartOnce = true;
			return State.START;
		}

		// Run in the ongrid state
		// Give command to make it on-grid
		context.component.setOngridCommand();
		// Set the grid mode to Ongrid
		context.component._setGridMode(GridMode.ON_GRID);

		// Do the softstart of the sinexcel
		CurrentState currentState = context.component.getSinexcelState();
		switch (currentState) {
		case UNDEFINED:
		case SLEEPING:
		case MPPT:
		case THROTTLED:
		case STARTED:
			context.component.softStart(true);
			break;
		case SHUTTINGDOWN:
		case FAULT:
		case STANDBY:
		case OFF:
		default:
			context.component.softStart(false);
		}

		return State.TOTAL_ONGRID;
	}
}
