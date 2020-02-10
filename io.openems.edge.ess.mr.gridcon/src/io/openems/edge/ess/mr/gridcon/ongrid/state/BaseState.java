package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.State;

public abstract class BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(BaseState.class);

	protected EssGridcon gridconPCS;
	
	public BaseState(EssGridcon gridconPCS) {
		this.gridconPCS = gridconPCS;
	}
	
	protected boolean isNextStateUndefined() {
		return false;
	}

	protected boolean isNextStateError() {
		return gridconPCS.isError();
	}

}
