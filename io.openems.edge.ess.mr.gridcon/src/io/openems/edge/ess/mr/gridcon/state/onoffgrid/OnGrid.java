package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateController;
import io.openems.edge.ess.mr.gridcon.StateObject;

public class OnGrid extends BaseState {

	private float targetFrequencyOnGrid;

	public OnGrid(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNa1, String inputNa2, String inputSyncBridge, String outputSyncBridge,
			String meterId, float targetFrequencyOnGrid, boolean na1Inverted, boolean na2Inverted,
			boolean inputSyncInverted) {
		super(manager, condition, gridconPcsId, b1Id, b2Id, b3Id, inputNa1, inputNa2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted);
		this.targetFrequencyOnGrid = targetFrequencyOnGrid;
	}

	@Override
	public IState getState() {
		return OnOffGridState.ON_GRID_MODE;
	}

	@Override
	public IState getNextState() {

		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}

		if (DecisionTableHelper.isOnGridMode(condition)) {
			return OnOffGridState.ON_GRID_MODE;
		}

		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}

		return OnOffGridState.ON_GRID_MODE;
	}

	@Override
	public void act() throws OpenemsNamedException {
		// handle sub state machine
		IState nextState = this.getSubStateObject().getNextState();
		StateObject nextStateObject = StateController.getStateObject(nextState);
		nextStateObject.setStateBefore(this.getSubStateObject().getState());

		System.out.println("  ----- CURRENT STATE:" + this.getSubStateObject().getState().getName());
		System.out.println("  ----- NEXT STATE:" + nextStateObject.getState().getName());

		this.setSubStateObject(nextStateObject);
		this.getSubStateObject().act();

		setSyncBridge(false);
		float factor = targetFrequencyOnGrid / GridconPcs.DEFAULT_GRID_FREQUENCY;
		System.out.println(" ---> set frequency factor: " + factor);
		getGridconPcs().setF0(factor);

		try {
			getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
