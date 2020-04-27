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
			String gridconPCS, //			
			String b1, //
			String b2, //
			String b3, //
			boolean enableIPU1, //
			boolean enableIPU2, //
			boolean enableIPU3, //
			ParameterSet parameterSet, //
			String hardRestartRelayAdress, //
			float offsetCurrent
			) {
	
		stateObjects = new HashMap<IState, StateObject>();

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, hardRestartRelayAdress));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, hardRestartRelayAdress, offsetCurrent));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPCS, b1, b2, b3, hardRestartRelayAdress);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined );
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, hardRestartRelayAdress));
		
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.UNDEFINED, new io.openems.edge.ess.mr.gridcon.state.ongrid.Undefined());
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ERROR, new io.openems.edge.ess.mr.gridcon.state.ongrid.Error());
		StateObject onGridStateObject = new io.openems.edge.ess.mr.gridcon.state.ongrid.OnGrid();
		onGridStateObject.setSubStateObject(gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState.ONGRID, onGridStateObject );		
	}

	public static StateObject getStateObject(IState state) {
		return stateObjects.get(state);
	}

	public static void initDecisionTableCondition(DecisionTableCondition tableCondition) {
		condition = tableCondition;
	}
	
	public static void initOnOffGrid(ComponentManager manager, String gridconPCS, String b1, String b2,
			String b3, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet,
			String inputNAProtection1, boolean na1Inverted, String inputNAProtection2, boolean na2Inverted,
			String inputSyncDeviceBridge, boolean inputSyncDeviceBridgeInverted, String outputSyncDeviceBridge,
			boolean outputSyncDeviceBridgeInverted, String outputHardReset, boolean outputHardResetInverted,
			float targetFrequencyOnGrid, float targetFrequencyOffGrid, String meterId, float deltaFrequency, float deltaVoltage, //
			float offsetCurrent) {
		
		System.out.println("INIT state controller: offset current is: " + offsetCurrent);
		
		
		stateObjects = new HashMap<IState, StateObject>();
		
		// State objects for gridcon in ongrid mode
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, outputHardReset));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, outputHardReset, offsetCurrent));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPCS, b1, b2, b3, outputHardReset);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined );
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, outputHardReset));
		
		
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.UNDEFINED, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.Undefined(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.START_SYSTEM, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.StartSystem(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.WAIT_FOR_DEVICES, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.WaitForDevices(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		
		OnGrid onGrid = new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnGrid(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, targetFrequencyOnGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted);
		onGrid.setSubStateObject(gridconUndefined);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.ON_GRID_MODE, onGrid);
		
		
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OffGrid(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, targetFrequencyOffGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_GRID_BACK, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.OffGridGridBack(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, targetFrequencyOffGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.WaitForGridAvailable(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, targetFrequencyOffGrid, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.OFF_GRID_MODE_ADJUST_PARMETER, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.AdjustParameter(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId, deltaFrequency, deltaVoltage, na1Inverted, na2Inverted, inputSyncDeviceBridgeInverted));
		
		
//		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.ERROR, new io.openems.edge.ess.mr.gridcon.onoffgrid.state.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId));
		
	}
	
	public static void printCondition() {
		System.out.println("condition: \n" + condition);
	}

}
