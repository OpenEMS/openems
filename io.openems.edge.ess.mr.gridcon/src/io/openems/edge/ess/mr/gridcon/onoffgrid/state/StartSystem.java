package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState;

public class StartSystem extends BaseState implements StateObject {

	public StartSystem(ComponentManager manager, DecisionTableCondition condition, String gridconPCSId, String b1Id,
			String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge,
			String meterId) {
		super(manager, condition, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge,
				meterId);
	}


	@Override
	public IState getState() {
		return OnOffGridState.START_SYSTEM;
	}

	@Override
	public IState getNextState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void act() throws OpenemsNamedException {
		// TODO Auto-generated method stub

	}

}
