package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class FullCharge  extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public FullCharge(//
			ManagedSymmetricEss ess //
	) {
		super(ess);
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State getNextState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void act() throws OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}

	
	

}
