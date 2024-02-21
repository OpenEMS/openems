package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;

public class UndefinedHandler extends StateHandler<CoolingUnitState, CoolingUnitContext> {
	private final Logger log = LoggerFactory.getLogger(UndefinedHandler.class);

	@Override
	public CoolingUnitState runAndGetNextState(CoolingUnitContext context) {
		final var batteries = context.batteries;
		final var coolingUnitErrorChannel = context.inputCoolingUnitErrorChannel;

		var coolingUnitError = coolingUnitErrorChannel.value();
		if (batteries.isEmpty()) {
			context.logWarn(this.log, "Battery list is empty!");
			return CoolingUnitState.UNDEFINED;
		}

		if (!coolingUnitError.isDefined() || coolingUnitError.get()) {
			context.logWarn(this.log, "Cooling unit error state not defined or has fault");
			return CoolingUnitState.UNDEFINED;
		}
		return CoolingUnitState.WAIT_FOR_START_REQUEST;
	}
}
