package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnGrid;

public class StateController {

	private static Map<IState, StateObject> stateObjects;

	private static DecisionTableCondition condition;

	public static void initOnGrid(//
			ComponentManager manager, //
			String gridconPcs, //
			String b1, //
			String b2, //
			String b3, //
			boolean enableI1, //
			boolean enableI2, //
			boolean enableI3, //
			ParameterSet parameterSet, //
			String hardRestartRelayAdress, //
			float offsetCurrent) {

		stateObjects = new HashMap<IState, StateObject>();

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPcs, b1, b2, b3,
						enableI1, enableI2, enableI3, parameterSet, hardRestartRelayAdress));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, parameterSet, hardRestartRelayAdress, offsetCurrent));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager,
				gridconPcs, b1, b2, b3, hardRestartRelayAdress);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPcs, b1, b2, b3, enableI1,
						enableI2, enableI3, parameterSet, hardRestartRelayAdress));

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.Undefined());
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.ongrid.Error());
		StateObject onGridStateObject = new io.openems.edge.ess.mr.gridcon.state.ongrid.OnGrid();
		onGridStateObject.setSubStateObject(gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ONGRID, onGridStateObject);
	}

	public static StateObject getStateObject(IState state) {
		return stateObjects.get(state);
	}

	public static void initDecisionTableCondition(DecisionTableCondition tableCondition) {
		condition = tableCondition;
	}

	public static void initOnOffGrid(ComponentManager manager, String gridconPcs, String b1, String b2, String b3,
			boolean enableIpu1, boolean enableIpu2, boolean enableIpu3, ParameterSet parameterSet,
			String inputNaProtection1, boolean na1Inverted, String inputNaProtection2, boolean na2Inverted,
			String inputSyncDeviceBridge, boolean inputSyncDeviceBridgeInverted, String outputSyncDeviceBridge,
			boolean outputSyncDeviceBridgeInverted, String outputHardReset, boolean outputHardResetInverted,
			float targetFrequencyOnGrid, float targetFrequencyOffGrid, String meterId, float deltaFrequency,
			float deltaVoltage, //
			float offsetCurrent) {

		System.out.println("INIT state controller: offset current is: " + offsetCurrent);

		stateObjects = new HashMap<IState, StateObject>();

		// State objects for gridcon in ongrid mode
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPcs, b1, b2, b3,
						enableIpu1, enableIpu2, enableIpu3, parameterSet, outputHardReset));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPcs, b1, b2, b3, enableIpu1,
						enableIpu2, enableIpu3, parameterSet, outputHardReset, offsetCurrent));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager,
				gridconPcs, b1, b2, b3, outputHardReset);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR,
				new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPcs, b1, b2, b3, enableIpu1,
						enableIpu2, enableIpu3, parameterSet, outputHardReset));

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.UNDEFINED,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.Undefined(manager, condition, gridconPcs, b1, b2, b3,
						inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId,
						na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.START_SYSTEM,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.StartSystem(manager, condition, gridconPcs, b1, b2,
						b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge,
						meterId, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.WAIT_FOR_DEVICES,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.WaitForDevices(manager, condition, gridconPcs, b1,
						b2, b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge,
						meterId, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));

		OnGrid onGrid = new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnGrid(manager, condition, gridconPcs, b1,
				b2, b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId,
				targetFrequencyOnGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted);
		onGrid.setSubStateObject(gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.ON_GRID_MODE, onGrid);

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OffGrid(manager, condition, gridconPcs, b1, b2, b3,
						inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId,
						targetFrequencyOffGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_GRID_BACK,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OffGridGridBack(manager, condition, gridconPcs, b1,
						b2, b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge,
						meterId, targetFrequencyOffGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(
				io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.WaitForGridAvailable(manager, condition, gridconPcs,
						b1, b2, b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge,
						outputSyncDeviceBridge, meterId, targetFrequencyOffGrid, na1Inverted, na2Inverted,
						inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER,
				new io.openems.edge.ess.mr.gridcon.state.onoffgrid.AdjustParameter(manager, condition, gridconPcs, b1,
						b2, b3, inputNaProtection1, inputNaProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge,
						meterId, deltaFrequency, deltaVoltage, na1Inverted, na2Inverted,
						inputSyncDeviceBridgeInverted));
	}

	public static void printCondition() {
		System.out.println("condition: \n" + condition);
	}

}
