package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;

public class WaitForStartRequestHandler extends StateHandler<CoolingUnitState, CoolingUnitContext> {
	private final Logger log = LoggerFactory.getLogger(WaitForStartRequestHandler.class);

	@Override
	protected void onEntry(CoolingUnitContext context) throws OpenemsNamedException {
		// Reset the cooling unit state
		context.outputCoolingUnitEnableChannel.setNextWriteValue(false);
	}

	@Override
	public CoolingUnitState runAndGetNextState(CoolingUnitContext context) {
		final var batteries = context.batteries;
		final var coolinUnitErrorChannel = context.inputCoolingUnitErrorChannel;
		var coolingUnitError = coolinUnitErrorChannel.value();

		if (!coolingUnitError.isDefined() || coolingUnitError.get()) {
			context.logWarn(this.log, "Cooling unit error state not defined or has fault");
			return CoolingUnitState.WAIT_FOR_START_REQUEST;
		}
		var isStartCoolingRequested = batteries.stream()//
				.map(BatteryFeneconF2b::getCoolingValveState)//
				.filter(Value::isDefined)//
				.anyMatch(Value::get);
		if (isStartCoolingRequested) {
			return CoolingUnitState.START_COOLING;
		}

		return CoolingUnitState.WAIT_FOR_START_REQUEST;
	}
}
