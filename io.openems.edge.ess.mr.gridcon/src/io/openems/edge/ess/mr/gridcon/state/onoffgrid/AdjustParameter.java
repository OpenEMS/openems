package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;

public class AdjustParameter extends BaseState {

	private float deltaVoltage;
	private float deltaFrequency;

	public AdjustParameter(ComponentManager manager, DecisionTableCondition condition, String gridconPcsId, String b1Id,
			String b2Id, String b3Id, String inputNa1, String inputNa2, String inputSyncBridge, String outputSyncBridge,
			String meterId, float deltaFrequency, float deltaVoltage, boolean na1Inverted, boolean na2Inverted,
			boolean inputSyncInverted) {
		super(manager, condition, gridconPcsId, b1Id, b2Id, b3Id, inputNa1, inputNa2, inputSyncBridge, outputSyncBridge,
				meterId, na1Inverted, na2Inverted);

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

		return OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER;
	}

	@Override
	public void act() throws OpenemsNamedException {

		float targetFrequency = getFrequencyOnMeter() + this.deltaFrequency;
		float frequencyFactor = targetFrequency / GridconPcs.DEFAULT_GRID_FREQUENCY;
		this.getGridconPcs().setF0(frequencyFactor);

		float targetVoltage = getVoltageOnMeter() + this.deltaVoltage;
		float voltageFactor = targetVoltage / GridconPcs.DEFAULT_GRID_VOLTAGE;
		this.getGridconPcs().setU0(voltageFactor);

		try {
			this.getGridconPcs().doWriteTasks();
		} catch (Exception e) {
			System.out.println("Adjust parameter , error while writing the tasks");
		}

	}

	@Override
	public GridconSettings getGridconSettings() {
		// TODO Auto-generated method stub
		return null;
	}

}
