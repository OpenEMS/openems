package io.openems.edge.ess.mr.gridcon.ongrid.state;

import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public abstract class BasteState implements StateObject{


	private StateObject subStateObject;

	@Override
	public IState getStateBefore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStateBefore(IState stateBefore) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void setSubStateObject(StateObject subStateObject) {
		this.subStateObject = subStateObject;
	}

	@Override
	public StateObject getSubStateObject() {
		return subStateObject;
	}
	
}
