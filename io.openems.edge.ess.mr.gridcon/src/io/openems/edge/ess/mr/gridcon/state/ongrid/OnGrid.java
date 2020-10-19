package io.openems.edge.ess.mr.gridcon.state.ongrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateController;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class OnGrid extends BasteState {

	@Override
	public IState getState() {
		return OnGridState.ONGRID;
	}

	@Override
	public IState getNextState() {
		return OnGridState.ONGRID; // Currently it is ot defined, so it is always ongrid
	}

	@Override
	public void act() throws OpenemsNamedException {
		// handle sub state machine
		IState nextState = this.getSubStateObject().getNextState();
		StateObject nextStateObject = StateController.getStateObject(nextState);
		nextStateObject.setStateBefore(this.getSubStateObject().getState());

		System.out.println("  ----- CURRENT STATE:" + this.getSubStateObject().getState().getName());
		System.out.println("  ----- NEXT STATE:" + nextStateObject.getState().getName());

		this.setSubStateObject(nextStateObject);
		this.getSubStateObject().act();

	}
}
