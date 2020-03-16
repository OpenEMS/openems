package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;

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
			ParameterSet parameterSet //
			) {
	
		stateObjects = new HashMap<IState, StateObject>();

		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPCS, b1, b2, b3);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined );
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		
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
			float targetFrequencyOnGrid, float targetFrequencyOffGrid, String meterId) {
		
		float targetFrequencyFactorOffGrid = targetFrequencyOffGrid / 50.0f;
		float targetFrequencyFactorOnGrid = targetFrequencyOnGrid / 50.0f;
		
		stateObjects = new HashMap<IState, StateObject>();
		
		// State objects for gridcon in ongrid mode
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		StateObject gridconUndefined = new io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined(manager, gridconPCS, b1, b2, b3);
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED, gridconUndefined );
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR, new io.openems.edge.ess.mr.gridcon.state.gridconstate.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		
		
		
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.UNDEFINED, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.Undefined(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState.START_SYSTEM, new io.openems.edge.ess.mr.gridcon.state.onoffgrid.StartSystem(manager, condition, gridconPCS, b1, b2, b3, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId));
		
		//TODO sub state object for RUN_ONGRID is sub state machine for gridcon
		// runOngrid = new RunOnGrid();
		//runOngrid.setSubStateObject(gridconUndefined);
		
//		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_ONGRID, new io.openems.edge.ess.mr.gridcon.onoffgrid.state.RunOnGrid(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, targetFrequencyOnGrid, meterId));
//		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.OFFGRID, new io.openems.edge.ess.mr.gridcon.onoffgrid.state.RunOffGrid(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, targetFrequencyOffGrid, meterId));
//		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.GOING_ONGRID, new io.openems.edge.ess.mr.gridcon.onoffgrid.state.RunGoingOnGrid(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, targetFrequencyOffGrid, meterId));
//		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.ERROR, new io.openems.edge.ess.mr.gridcon.onoffgrid.state.Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, inputNAProtection1, inputNAProtection2, inputSyncDeviceBridge, outputSyncDeviceBridge, meterId));
		
	}

}
