package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;

public class AdjustParameter extends BaseState {

	private float deltaVoltage;
	private float deltaFrequency;

	public AdjustParameter(ComponentManager manager, DecisionTableCondition condition, String gridconPCSId, String b1Id,
			String b2Id, String b3Id, String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge,
			String meterId, float deltaFrequency, float deltaVoltage) {
		super(manager, condition, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge,
				meterId);

		this.deltaFrequency = deltaFrequency;
		this.deltaVoltage = deltaVoltage;
	}

	@Override
	public IState getState() {
		return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
	}

	@Override
	public IState getNextState() {
		
		if (DecisionTableHelper.isUndefined(condition)) {
			return OnOffGridState.UNDEFINED;
		}
		
		if (DecisionTableHelper.isAdjustParameters(condition)) {
			return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
		}
		
		if (DecisionTableHelper.isOffGridMode(condition)) {
			return OnOffGridState.OFF_GRID_MODE;
		}
		
		if (DecisionTableHelper.isOnGridMode(condition)) {
			return OnOffGridState.ON_GRID_MODE;
		}
		
//		if (DecisionTableHelper.isRestartGridconAfterSync(condition)) {
//			return OnOffGridState.ON_GRID_RESTART_GRIDCON_AFTER_SYNC;
//		}
		
		return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
	}

	@Override
	public void act() throws OpenemsNamedException {
		
		float targetFrequency = getFrequencyOnMeter() + deltaFrequency;
		float frequencyFactor = targetFrequency  / GridconPCS.DEFAULT_GRID_FREQUENCY;
		getGridconPCS().setF0(frequencyFactor);
		
		float targetVoltage = getVoltageOnMeter() + deltaVoltage;
		float voltageFactor = targetVoltage  / GridconPCS.DEFAULT_GRID_VOLTAGE;
		getGridconPCS().setU0(voltageFactor);
		
		try {
			getGridconPCS().doWriteTasks();
		} catch (Exception e) {
			System.out.println("Adjust parameter , error while writing the tasks");
		}
		
	}

}
