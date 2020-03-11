package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class Undefined extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id,
			String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge, String meterId) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge, meterId);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can be STOPPED, ERROR, RUN or UNDEFINED
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.UNDEFINED;
		}
		
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.ERROR;
		}
		
		if (isNextStateOnGridRunning()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_ONGRID;
		}
		
		if (isNextStateOnGridStopped()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.STOPPED;
		}
		
		if(isNextStateOffGrid()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.OFFGRID;
		}
		
		if (isNextStateGoingOnGrid()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.GOING_ONGRID;
		}
		
		return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.UNDEFINED;
	}

	@Override
	public void act() {
		log.info("Nothing to do!");
	}
}
