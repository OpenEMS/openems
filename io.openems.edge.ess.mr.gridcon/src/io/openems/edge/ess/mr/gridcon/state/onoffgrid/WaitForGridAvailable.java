package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;

public class WaitForGridAvailable extends BaseState {

	//TODO in diesem state darf ich max. 30 sekunden verbleiben
	// danach muss ich den mr runterfahren...=> outputHardresetrelay für mr setzen
	
	private float targetFrequencyOffgrid;

	public WaitForGridAvailable(ComponentManager manager, DecisionTableCondition condition, String gridconPCSId,
			String b1Id, String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge,
			String outputSyncBridge, String meterId, float targetFrequencyOffgrid, boolean na1Inverted, boolean na2Inverted, boolean inputSyncInverted) {
		super(manager, condition, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted, inputSyncInverted);
			this.targetFrequencyOffgrid = targetFrequencyOffgrid;
	}

	@Override
	public IState getState() {
		return OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE;
	}

	@Override
	public IState getNextState() {
		
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}
		
		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}
		
		if (DecisionTableHelper.isOffGridWaitForGridAvailable(condition)) {
			return OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE;
		}
		
		if (DecisionTableHelper.isAdjustParameters(condition)) {
			return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
		}
		
//		if (DecisionTableHelper.isRestartGridconAfterSync(condition)) {
//			return OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC;
//		}
		
		return OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE;
		
	}

	@Override
	public void act() throws OpenemsNamedException {
		float factor = targetFrequencyOffgrid / GridconPCS.DEFAULT_GRID_FREQUENCY;
		getGridconPCS().setF0(factor);
		try {
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
