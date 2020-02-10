package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;

public class Undefined extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(EssGridcon gridconPCS) {
		super(gridconPCS);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be Stopped, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
		}
		
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
		}
		
		if (gridconPCS.isRunning()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
	}

	

	@Override
	public void act() {
		log.info("Nothing to do!");
	}
}
