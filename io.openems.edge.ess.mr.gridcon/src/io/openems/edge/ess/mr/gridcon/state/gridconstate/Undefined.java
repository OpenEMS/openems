package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class Undefined extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id,
			String hardRestartRelayAdress) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can be STOPPED, ERROR, RUN or
		// UNDEFINED
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
		}

		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR;
		}

		if (isNextStateRunning()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN;
		}

		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED;
		}

		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
	}

	@Override
	public void act() {
		log.info("undefined.act() -> Nothing to do!");
	}
}
