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
			boolean ena1, //
			boolean ena2, //
			boolean ena3, //
			ParameterSet parameterSet //
			) {
	
		stateObjects = new HashMap<IState, StateObject>();

		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED, new Undefined(manager, gridconPCS, b1, b2, b3));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.STOPPED, new Stopped(manager, gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.RUN, new Run(manager, gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.ERROR, new Error(manager, gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));

	}

	public static StateObject getStateObject(IState state) {
		return stateObjects.get(state);
	}

}
