package io.openems.edge.ess.sinexcel.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class StopInverterHandler extends StateHandler<State, Context> {
	
	private final Logger log = LoggerFactory.getLogger(StopInverterHandler.class);

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		
		log.info("Inside  StopInverter handler");
		//Stop the inverter no matter what
		context.component.inverterOff();
		
		//go to grounding set step after stopping
		return State.GROUNDSET;
		
	}

}
