package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;

public class Undefined extends BaseState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	private int timeSecondsToWaitWhenUndefined = 70;
	private LocalDateTime lastMethodCall;

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

		io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState stateToReturn = io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;

		if (this.lastMethodCall == null) {
			this.lastMethodCall = LocalDateTime.now();
		}

		if (this.lastMethodCall.plusSeconds(this.timeSecondsToWaitWhenUndefined).isBefore(LocalDateTime.now())) {

			if (isNextStateUndefined()) {
				stateToReturn = io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
			}

			if (isNextStateError()) {
				stateToReturn = io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR;
			}

			if (isNextStateRunning()) {
				stateToReturn = io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN;
			}

			if (isNextStateStopped()) {
				stateToReturn = io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED;
			}
		}
		if (stateToReturn != io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED) {
			this.lastMethodCall = null;
		}

		return stateToReturn;
	}

	@Override
	public void act(GridconSettings settings) {
		this.log.info("undefined.act() -> Nothing to do!");
	}
}
