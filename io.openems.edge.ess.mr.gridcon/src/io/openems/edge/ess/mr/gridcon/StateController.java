package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Error;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Run;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Stopped;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Undefined;

public class StateController {

	private static Map<IState, State> stateObjects;

	public static void initOnGrid(//
			GridconPCS gridconPCS, //			
			SoltaroBattery b1, //
			SoltaroBattery b2, //
			SoltaroBattery b3, //
			boolean ena1, //
			boolean ena2, //
			boolean ena3, //
			ParameterSet parameterSet //
			) {
	
		stateObjects = new HashMap<IState, State>();

		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED, new Undefined(gridconPCS, b1, b2, b3));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED, new Stopped(gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.RUN, new Run(gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR, new Error(gridconPCS, b1, b2, b3, ena1, ena2, ena3, parameterSet));

	}

	public static State getStateObject(IState state) {
		return stateObjects.get(state);
	}

}
