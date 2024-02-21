package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;

public class WaitForStopRequestHandler extends StateHandler<CoolingUnitState, CoolingUnitContext> {

	@Override
	public CoolingUnitState runAndGetNextState(CoolingUnitContext context) {
		final var batteries = context.batteries;
		var isStopCoolingRequested = batteries.stream()//
				.map(BatteryFeneconF2b::getCoolingValveState)//
				.filter(Value::isDefined)//
				.allMatch(t -> !t.get());
		if (isStopCoolingRequested) {
			return CoolingUnitState.STOP_COOLING;
		}
		return CoolingUnitState.WAIT_FOR_STOP_REQUEST;
	}
}
