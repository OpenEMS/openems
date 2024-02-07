package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;

public class StopCoolingHandler extends StateHandler<CoolingUnitState, CoolingUnitContext> {

	@Override
	public CoolingUnitState runAndGetNextState(CoolingUnitContext context) throws OpenemsNamedException {
		context.outputCoolingUnitEnableChannel.setNextWriteValue(false);
		return CoolingUnitState.WAIT_FOR_START_REQUEST;
	}
}
