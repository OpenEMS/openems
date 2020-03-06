package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Error;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Run;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Stopped;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Undefined;

public class StateController {

	private static Map<IState, StateObject> stateObjects;

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

		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED, new Undefined(manager, gridconPCS, b1, b2, b3));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.STOPPED, new Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.RUN, new Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.ERROR, new Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));

	}

	public static StateObject getStateObject(IState state) {
		return stateObjects.get(state);
	}

	public static void initOnOffGrid(ComponentManager manager, String gridconPCS, String b1, String b2,
			String b3, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet,
			String inputNAProtection1, boolean na1Inverted, String inputNAProtection2, boolean na2Inverted,
			String inputSyncDeviceBridge, boolean inputSyncDeviceBridgeInverted, String outputSyncDeviceBridge,
			boolean outputSyncDeviceBridgeInverted, String outputHardReset, boolean outputHardResetInverted,
			double targetFrequencyOnGrid, double targetFrequencyOffGrid) {
		// TODO Auto-generated method stub
		
		stateObjects = new HashMap<IState, StateObject>();
		
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.UNDEFINED, new Undefined(manager, gridconPCS, b1, b2, b3));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.STOPPED, new Stopped(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_ONGRID, new Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet, targetFrequencyOnGrid));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_OFFGRID, new Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_GOING_ONGRID, new Run(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.ERROR, new Error(manager, gridconPCS, b1, b2, b3, enableIPU1, enableIPU2, enableIPU3, parameterSet));
		
	}

}
