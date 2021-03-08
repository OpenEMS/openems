package io.openems.edge.ess.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.sinexcel.CurrentState;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotalOnGridHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(TotalOnGridHandler.class);

	protected boolean StartOnce = false;

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		log.info("inside total ongrid state");
		if (context.getGridDetector().get()) {
			return State.STOP;
		}

		context.component.inverterOn();
		context.component.softStart(true);
		boolean isInvOn = context.component.stateOnOff();
		log.info("Is inverter on ? :" + isInvOn);
		Value<Integer> freq = context.component.getFrequency();
		log.info("frequency is : " + freq.get() + " hz");
		
		
		if (isInvOn) {
			// Inverter is on


			// 1. Give command to make it on-grid
			context.component.setOngridCommand();

				 // do this before , make it undefined
				
				// 2. Set the grid mode to Ongrid
				context.component._setGridMode(GridMode.ON_GRID);

		} else {
			log.info("Inverter is not on , going back to swithc on inverter");
			return State.TOTAL_ONGRID;
		}	
		

		// Run in the ongrid state

		// 1. Give command to make it on-grid
		context.component.setOngridCommand();

		// 2. Set the grid mode to Ongrid
		context.component._setGridMode(GridMode.ON_GRID);

		// 3. Do the softstart of the sinexcel
		CurrentState currentState = context.component.getSinexcelState();

		switch (currentState) {
		case UNDEFINED:
		case SLEEPING:
		case MPPT:
		case THROTTLED:
		case STARTED:
		case STANDBY:
			context.component.softStart(true);
			break;
		case SHUTTINGDOWN:
		case FAULT:
		case OFF:
		default:
			context.component.softStart(false);
		}
		return State.TOTAL_ONGRID;
	}
}
