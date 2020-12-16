package io.openems.edge.ess.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {	

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		State decisionVariable = context.stateTransitionHelper();
		switch (decisionVariable) {
		case ERROR:
			return State.ERROR;
			
		case GROUNDSET:
			return State.GROUNDSET;
		
		case TOTAL_OFFGRID:
			return State.TOTAL_OFFGRID;
			
		case TOTAL_ONGRID:
			return State.TOTAL_ONGRID;
			
		case UNDEFINED:
			return State.UNDEFINED;
		
		// This should never happen	
		case START:			
		case STOP:		
			break;
		
		}		
		// This should never happen too
		return State.UNDEFINED;

	}

	
	
}
