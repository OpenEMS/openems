package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class Undefined extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNa1, String inputNa2, String inputSyncBridge, String outputSyncBridge,
			String meterId, boolean na1Inverted, boolean na2Inverted, boolean inputSyncInverted) {
		super(manager, condition, gridconPcsId, b1Id, b2Id, b3Id, inputNa1, inputNa2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.UNDEFINED;
	}

	@Override
	public IState getNextState() {
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}

		if (DecisionTableHelper.isStateStartSystem(condition)) {
			return OnOffGridState.START_SYSTEM;
		}

		if (DecisionTableHelper.isWaitingForDevices(condition)) {
			return OnOffGridState.WAIT_FOR_DEVICES;
		}

		if (DecisionTableHelper.isOnGridMode(condition)) {
			System.out.println("DecisionTableHelper -->  On grid conditions!");
			return OnOffGridState.ON_GRID_MODE;
		}

		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}

		if (DecisionTableHelper.isOffGridGridBack(condition)) {
			return OnOffGridState.OFF_GRID_MODE_GRID_BACK;
		}

		if (DecisionTableHelper.isOffGridWaitForGridAvailable(condition)) {
			return OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE;
		}

		if (DecisionTableHelper.isAdjustParameters(condition)) {
			return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
		}

		return OnOffGridState.UNDEFINED;
	}

	@Override
	public void act() {
		this.log.info("Nothing to do!");
	}

	@Override
	public GridconSettings getGridconSettings() {
		// TODO Auto-generated method stub
		return null;
	}
}
