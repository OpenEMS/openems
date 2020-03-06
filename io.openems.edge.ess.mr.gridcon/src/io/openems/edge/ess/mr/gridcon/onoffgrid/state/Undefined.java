package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class Undefined extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can be STOPPED, ERROR, RUN or UNDEFINED
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED;
		}
		
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.ERROR;
		}
		
		if (isNextStateRunning()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.RUN;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED;
	}

	@Override
	public void act() {
		log.info("Nothing to do!");
	}
}
