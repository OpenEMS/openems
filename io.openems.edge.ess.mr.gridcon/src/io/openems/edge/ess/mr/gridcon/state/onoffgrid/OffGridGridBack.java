package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;

public class OffGridGridBack extends BaseState {

	private float targetFrequencyOffgrid;

	public OffGridGridBack(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNa1, String inputNa2, String inputSyncBridge, String outputSyncBridge,
			String meterId, float targetFrequencyOffgrid, boolean na1Inverted, boolean na2Inverted,
			boolean inputSyncInverted) {
		super(manager, condition, gridconPcsId, b1Id, b2Id, b3Id, inputNa1, inputNa2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted);
		this.targetFrequencyOffgrid = targetFrequencyOffgrid;
	}

	@Override
	public IState getState() {
		return OnOffGridState.OFF_GRID_MODE_GRID_BACK;
	}

	@Override
	public IState getNextState() {
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}

		if (DecisionTableHelper.isOffGridGridBack(condition)) {
			return OnOffGridState.OFF_GRID_MODE_GRID_BACK;
		}

		if (DecisionTableHelper.isOffGridWaitForGridAvailable(condition)) {
			return OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE;
		}

		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}

		return OnOffGridState.OFF_GRID_MODE_GRID_BACK;
	}

	@Override
	public void act() throws OpenemsNamedException {
		setSyncBridge(true);
		float factor = this.targetFrequencyOffgrid / GridconPcs.DEFAULT_GRID_FREQUENCY;
		this.getGridconPcs().setF0(factor);
		try {
			this.getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public GridconSettings getGridconSettings() {
		// TODO Auto-generated method stub
		return null;
	}

}
