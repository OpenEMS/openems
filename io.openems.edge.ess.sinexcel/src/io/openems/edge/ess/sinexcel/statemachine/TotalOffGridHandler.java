package io.openems.edge.ess.sinexcel.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.sinexcel.CurrentState;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class TotalOffGridHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(TotalOffGridHandler.class);

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		log.info("Inside total offgrid handler");

		if (!context.getGridDetector().get()) {
			return State.STOP;
		}
		context.component.setclearFailureCommand();
		// Set start command to inverter
		context.component.inverterOn();
		context.component.softStart(true);
		boolean isInvOn = context.component.stateOnOff();
		log.info("Is inverter on ? :" + isInvOn);

		context.component.setFrequency();
		Value<Integer> freq = context.component.getFrequency();
		log.info("frequency is : " + freq.get() + " hz");
		
		
		context.component.setOffgridCommand();

		// 3. Set the grid mode to Offgrid , // do this before , make it undefined
		context.component._setGridMode(GridMode.OFF_GRID);
		
		if (isInvOn) {
			// Inverter is on

			// 1. Set the frequncy
			context.component.setFrequency();

			if (freq.get() == 52) {

				// 2. Give command to make it off-grid
				context.component.setOffgridCommand();

				// 3. Set the grid mode to Offgrid , // do this before , make it undefined
				context.component._setGridMode(GridMode.OFF_GRID);
			} else {
				log.info("frequency is not 52 yet, goign back again to check freq");
				return State.TOTAL_OFFGRID;
			}

		} else {
			log.info("Inverter is not on, going back to swithc on inverter");
			return State.TOTAL_OFFGRID;
		}

		// Run in the offgrid state

//		// 1. Set the frequncy
//		context.component.setFrequency();
//		
//		// 2. Give command to make it off-grid
//		context.component.setOffgridCommand();
//		
//		// 3. Set the grid mode to Offgrid , // do this before , make it undefined
//		context.component._setGridMode(GridMode.OFF_GRID);

		// 4. Do the softstart of the sinexcel
		
		log.info("parameters are set for the inverter to the softstart");
		
		
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

		return State.TOTAL_OFFGRID;

	}
}
