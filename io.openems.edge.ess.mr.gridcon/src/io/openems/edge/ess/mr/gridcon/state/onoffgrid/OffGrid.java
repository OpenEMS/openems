package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.WeightingHelper;
import io.openems.edge.ess.mr.gridcon.enums.Mode;

public class OffGrid extends BaseState {

	private float targetFrequencyOffgrid;

	public OffGrid(ComponentManager manager, DecisionTableCondition condition, String gridconPCSId, String b1Id,
			String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge,
			String meterId, float targetFrequencyOffgrid) {
		super(manager, condition, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge,
				meterId);
		this.targetFrequencyOffgrid = targetFrequencyOffgrid;
	}

	@Override
	public IState getState() {
		return OnOffGridState.OFF_GRID_MODE;
	}

	@Override
	public IState getNextState() {
		
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}
		
		if (DecisionTableHelper.isOffGridGridBack(condition)) {
			return OnOffGridState.OFF_GRID_MODE_GRID_BACK;
		}
		
		if (DecisionTableHelper.isAdjustParameters(condition)) {
			return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
		}
		
		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}
		
		return OnOffGridState.OFF_GRID_MODE;
	}

	@Override
	public void act() throws OpenemsNamedException {
		float factor = targetFrequencyOffgrid / GridconPCS.DEFAULT_GRID_FREQUENCY;
		getGridconPCS().setF0(factor);
		
		getGridconPCS().setBlackStartApproval(true);
		getGridconPCS().setSyncApproval(false);
		getGridconPCS().setModeSelection(Mode.VOLTAGE_CONTROL);
		
		// Set weighting to strings, use a fix value for active power because in
		// off grid mode it is always discharging
		float activePower = 1000; 
		Float[] weightings = WeightingHelper.getWeighting(activePower, getBattery1(), getBattery2(), getBattery3());
		
		getGridconPCS().setWeightStringA(weightings[0]);
		getGridconPCS().setWeightStringB(weightings[1]);
		getGridconPCS().setWeightStringC(weightings[2]);
		
		try {
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
