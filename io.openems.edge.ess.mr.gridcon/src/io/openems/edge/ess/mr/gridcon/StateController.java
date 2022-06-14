// CHECKSTYLE:OFF
package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState;
import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconStateObject;
import io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState;
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
			// ParameterSet parameterSet, //
			String hardRestartRelayAdress, //
			float offsetCurrent) {

		this.generalStateObjects = new HashMap<IState, StateObject>();
		this.gridconStateObjects = new HashMap<IState, GridconStateObject>();

		this.gridconStateObjects.put(GridconState.STOPPED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress));
		this.gridconStateObjects.put(GridconState.RUN,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress, offsetCurrent));
		this.gridconStateObjects.put(GridconState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPcs, b1, b2, b3,
						hardRestartRelayAdress));
		this.gridconStateObjects.put(GridconState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, /* parameterSet, */ hardRestartRelayAdress));

		this.generalStateObjects.put(OnGridState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.Undefined());
		this.generalStateObjects.put(OnGridState.ERROR, new io.openems.edge.ess.mr.gridcon.state.ongrid.Error());
		this.generalStateObjects.put(OnGridState.ONGRID, new io.openems.edge.ess.mr.gridcon.state.ongrid.OnGrid());
	}

	public StateObject getGeneralStateObject(IState state) {
		return this.generalStateObjects.get(state);
	}

	public GridconStateObject getGridconStateObject(IState state) {
		return this.gridconStateObjects.get(state);
	}

	public void initDecisionTableCondition(DecisionTableCondition tableCondition) {
		this.condition = tableCondition;
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
		System.out.println("condition: \n" + this.condition);
	}

}
// CHECKSTYLE:ON
