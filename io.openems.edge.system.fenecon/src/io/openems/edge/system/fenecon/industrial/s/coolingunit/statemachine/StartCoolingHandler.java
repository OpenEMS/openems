package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;

public class StartCoolingHandler extends StateHandler<CoolingUnitState, CoolingUnitContext> {

	private static final int MIN_RUN_TIME = 6;// [minutes]
	private Instant lastStateChange;

	@Override
	protected void onEntry(CoolingUnitContext context) throws OpenemsNamedException {
		context.outputCoolingUnitEnableChannel.setNextWriteValue(true);
		this.lastStateChange = Instant.now(context.clock);
	}

	@Override
	public CoolingUnitState runAndGetNextState(CoolingUnitContext context) throws OpenemsNamedException {
		if (Instant.now(context.clock).minus(Duration.ofMinutes(MIN_RUN_TIME)).isAfter(this.lastStateChange)) {
			return CoolingUnitState.WAIT_FOR_STOP_REQUEST;
		}
		return CoolingUnitState.START_COOLING;
	}
}
