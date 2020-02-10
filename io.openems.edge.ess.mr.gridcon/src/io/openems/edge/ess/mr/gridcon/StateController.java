package io.openems.edge.ess.mr.gridcon;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Error;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Run;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Stopped;
import io.openems.edge.ess.mr.gridcon.ongrid.state.Undefined;

public class StateController {

	private static Map<IState, State> stateObjects;

	//public static void initOnGrid(ComponentManager componentManager, io.openems.edge.ess.mr.gridcon.ongrid.Config c) throws OpenemsNamedException {
	public static void initOnGrid(EssGridcon gridconPCS, io.openems.edge.ess.mr.gridcon.ongrid.Config c) throws OpenemsNamedException {
	
	stateObjects = new HashMap<IState, State>();

		

		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED, new Undefined(gridconPCS));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED, new Stopped(gridconPCS, c.enableIPU1(), c.enableIPU2(), c.enableIPU3(), c.parameterSet()));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.RUN, new Run(gridconPCS, c.enableIPU1(), c.enableIPU2(), c.enableIPU3(), c.parameterSet()));
		stateObjects.put(io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR, new Error(gridconPCS));

	}

	public static State getStateObject(IState state) {
		return stateObjects.get(state);
	}

}
