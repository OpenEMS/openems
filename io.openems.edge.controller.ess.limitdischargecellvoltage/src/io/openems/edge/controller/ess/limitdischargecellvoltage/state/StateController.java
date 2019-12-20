package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class StateController {
	
	private static Map<State, IState> stateObjects;
	
	public static void init( ComponentManager componentManager, Config c) throws OpenemsNamedException {
		stateObjects = new HashMap<State, IState>();
		
		ManagedSymmetricEss ess;
		
		ess = componentManager.getComponent(c.ess_id());
		stateObjects.put(State.UNDEFINED, new Undefined(ess, c.warningLowCellVoltage(), c.criticalHighCellVoltage(), c.warningSoC(), c.lowTemperature(), c.highTemperature()));
		stateObjects.put(State.NORMAL, new Normal(ess, c.warningLowCellVoltage(), c.criticalHighCellVoltage(), c.warningSoC(), c.lowTemperature(), c.highTemperature(), c.unusedTime()));
		stateObjects.put(State.LIMIT, new Limit(ess, c.warningLowCellVoltage(), c.criticalLowCellVoltage(), c.criticalHighCellVoltage(), c.warningSoC(), c.lowTemperature(), c.highTemperature(), c.unusedTime()));
		stateObjects.put(State.FORCE_CHARGE, new ForceCharge(ess, c.chargePowerPercent(), c.chargingTime()));
		stateObjects.put(State.FULL_CHARGE, new FullCharge(ess));
		stateObjects.put(State.CHECK, new Check(ess, c.deltaSoC()));

	}
	
	public static IState getStateObject(State state) {
		return stateObjects.get(state);
	}

}
