package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconStateObject;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;

public class StateController {

	private Map<IState, StateObject> generalStateObjects;
	private Map<IState, GridconStateObject> gridconStateObjects;

	private DecisionTableCondition condition;

	public StateController() {

	}

	public void initOnGrid(//
			ComponentManager manager, //
			String gridconPcs, //
			String b1, //
			String b2, //
			String b3, //
			boolean enableI1, //
			boolean enableI2, //
			boolean enableI3, //
//			ParameterSet parameterSet, //
			String hardRestartRelayAdress, //
			float offsetCurrent) {

		generalStateObjects = new HashMap<IState, StateObject>();
		gridconStateObjects = new HashMap<IState, GridconStateObject>();

		gridconStateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress));
		gridconStateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress, offsetCurrent));
		gridconStateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPcs, b1, b2, b3,
						hardRestartRelayAdress));
		gridconStateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress));

		generalStateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.Undefined());
		generalStateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.Error());
		generalStateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ONGRID,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.OnGrid());
	}

	public StateObject getGeneralStateObject(IState state) {
		return generalStateObjects.get(state);
	}

	public GridconStateObject getGridconStateObject(IState state) {
		return gridconStateObjects.get(state);
	}

	public void initDecisionTableCondition(DecisionTableCondition tableCondition) {
		condition = tableCondition;
	}

	public void initOnOffGrid(ComponentManager manager, String gridconPcs, String b1, String b2, String b3,
			boolean enableIpu1, boolean enableIpu2, boolean enableIpu3, ParameterSet parameterSet,
			String inputNaProtection1, boolean na1Inverted, String inputNaProtection2, boolean na2Inverted,
			String inputSyncDeviceBridge, boolean inputSyncDeviceBridgeInverted, String outputSyncDeviceBridge,
			boolean outputSyncDeviceBridgeInverted, String outputHardReset, boolean outputHardResetInverted,
			float targetFrequencyOnGrid, float targetFrequencyOffGrid, String meterId, float deltaFrequency,
			float deltaVoltage, //
			float offsetCurrent) {

	}

	public void printCondition() {
		System.out.println("condition: \n" + condition);
	}

}
