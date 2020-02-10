package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;

public class Error extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Error.class);

	public Error(EssGridcon gridconPCS) {
		super(gridconPCS);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be Stopped, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
		}
		return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
	}

	

	@Override
	public void act() {
		log.info("Handle Errors!");
		//TODO
	}
}
