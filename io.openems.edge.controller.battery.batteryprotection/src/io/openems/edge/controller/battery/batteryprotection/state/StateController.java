package io.openems.edge.controller.battery.batteryprotection.state;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.battery.batteryprotection.Config;
import io.openems.edge.controller.battery.batteryprotection.IState;
import io.openems.edge.controller.battery.batteryprotection.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class StateController {

	private static Map<State, IState> stateObjects;

	public static void init(ComponentManager componentManager, Config c) throws OpenemsNamedException {
		stateObjects = new HashMap<State, IState>();

		ManagedSymmetricEss ess;
		Battery bms;

		ess = componentManager.getComponent(c.ess_id());
		bms = componentManager.getComponent(c.bms_id());
		stateObjects.put(State.UNDEFINED, new Undefined(ess, bms));
		stateObjects.put(State.NORMAL, new Normal(ess, bms, c.warningLowCellVoltage(), c.criticalHighCellVoltage(),
				c.warningSoC(), c.lowTemperature(), c.highTemperature(), c.unusedTime()));
		stateObjects.put(State.LIMIT, new Limit(ess, bms, c.warningLowCellVoltage(), c.criticalLowCellVoltage(),
				c.criticalHighCellVoltage(), c.warningSoC(), c.criticalSoC(), c.lowTemperature(), c.highTemperature(), c.unusedTime()));
		stateObjects.put(State.FORCE_CHARGE, new ForceCharge(ess, bms, c.chargePowerPercent(), c.chargingTime(),
				c.forceChargeReachableMinCellVoltage(), c.warningSoC()));
		stateObjects.put(State.FULL_CHARGE, new FullCharge(ess, bms, c.criticalHighCellVoltage()));
		stateObjects.put(State.CHECK, new Check(ess, bms, c.deltaSoC(), c.unusedTime(), c.criticalLowCellVoltage()));

	}

	public static IState getStateObject(State state) {
		return stateObjects.get(state);
	}

}