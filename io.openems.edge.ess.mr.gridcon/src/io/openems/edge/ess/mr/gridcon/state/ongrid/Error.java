package io.openems.edge.ess.mr.gridcon.state.ongrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;

public class Error extends BasteState {

	@Override
	public IState getState() {
		return OnGridState.ERROR;
	}

	@Override
	public IState getNextState() {
		return OnGridState.ONGRID; // Currently it is not defined, so it is always ongrid
	}

	@Override
	public void act() throws OpenemsNamedException {
		// Nothing to do
	}

	@Override
	public GridconSettings getGridconSettings() {
		// TODO Auto-generated method stub
		return null;
	}
}
