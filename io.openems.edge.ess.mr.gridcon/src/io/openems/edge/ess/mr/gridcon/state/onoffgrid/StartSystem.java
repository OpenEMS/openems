package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class StartSystem extends BaseState implements StateObject {

	public StartSystem(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNa1, String inputNa2, String inputSyncBridge, String outputSyncBridge,
			String meterId, boolean na1Inverted, boolean na2Inverted, boolean inputSyncInverted) {
		super(manager, condition, gridconPcsId, b1Id, b2Id, b3Id, inputNa1, inputNa2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted);
	}

	@Override
	public IState getState() {
		return OnOffGridState.START_SYSTEM;
	}

	@Override
	public IState getNextState() {

		if (DecisionTableHelper.isWaitingForDevices(condition)) {
			return OnOffGridState.WAIT_FOR_DEVICES;
		}

		if (DecisionTableHelper.isError(condition)) {
			return OnOffGridState.ERROR;
		}

		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}

		return OnOffGridState.START_SYSTEM;
	}

	@Override
	public void act() throws OpenemsNamedException {
		setSyncBridge(false);
	}

	@Override
	public GridconSettings getGridconSettings() {
		// TODO Auto-generated method stub
		return null;
	}

}
