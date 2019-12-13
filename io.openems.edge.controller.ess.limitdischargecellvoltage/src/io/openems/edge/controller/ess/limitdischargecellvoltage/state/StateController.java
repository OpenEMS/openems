package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class StateController {
	
	private static Map<State, IState> stateObjects;
	
	public static void init( ComponentManager componentManager, Config config) throws OpenemsNamedException {
		stateObjects = new HashMap<State, IState>();
		
		ManagedSymmetricEss ess;
		
		ess = componentManager.getComponent(config.ess_id());
		
		stateObjects.put(State.UNDEFINED, new Undefined(ess, config));
		stateObjects.put(State.NORMAL, new Normal(ess, config));
		stateObjects.put(State.LIMIT, new Limit(ess, config));
		stateObjects.put(State.FORCE_CHARGE, new ForceCharge(ess, config));
		stateObjects.put(State.FULL_CHARGE, new FullCharge(ess, config));
		stateObjects.put(State.CHECK, new Check(ess, config));

	}
	
	public static IState getStateObject(State state) {
		return stateObjects.get(state);
	}

}
