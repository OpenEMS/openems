package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.IState;

public class WaitForDevices extends BaseState {

	public WaitForDevices(ComponentManager manager, DecisionTableCondition condition, String gridconPCSId, String b1Id,
			String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge,
			String meterId) {
		super(manager, condition, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge,
				meterId);
	}
	@Override
	public IState getState() {
		return OnOffGridState.WAIT_FOR_DEVICES;
	}

	@Override
	public IState getNextState() {
		
		if (DecisionTableHelper.isWaitingForDevices(condition)) {			
			return OnOffGridState.WAIT_FOR_DEVICES;
		}
		
		if (DecisionTableHelper.isOnGridMode(condition)) {			
			return OnOffGridState.ON_GRID_MODE;
		}
		
		if (DecisionTableHelper.isUndefined(condition)) {			
			return OnOffGridState.UNDEFINED;
		}		
		
		return OnOffGridState.WAIT_FOR_DEVICES;
	}

	@Override
	public void act() throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

}
