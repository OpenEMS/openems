package io.openems.edge.ess.mr.gridcon.ongrid.state;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.ongrid.OnGridState;

public class Undefined extends BasteState {

	@Override
	public IState getState() {
		return OnGridState.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		return OnGridState.ONGRID; //Currently it is ot defined, so it is always ongrid
	}

	@Override
	public void act() throws OpenemsNamedException {
		// Nothing to do		
	}

}
