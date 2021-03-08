package io.openems.edge.ess.sinexcel.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class StartInverterHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(StartInverterHandler.class);

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		log.info("Inside  StartInverter handler");
		
		// Start the inverter
		context.component.inverterOn();
		
		boolean isInvOn = context.component.stateOnOff();
		log.info("Is inverter on ? :" + isInvOn);
		
		if (!isInvOn) {
			return State.START;
		}

		// Started the inverter, Check the grid detector
		// If Grid is off
		if (context.getGridDetector().get()) {
			return State.TOTAL_OFFGRID;
		} else {
			return State.TOTAL_ONGRID;
		}
	}
}
