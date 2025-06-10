package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) {
		this.entryAt = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		// Due to the low battery DC voltage, the inverter receives a battery low
		// voltage error in the stop process and goes into a fault state that can be
		// ignored during this time. Due to this situation, hasFault is preferred
		// instead of hasFailure.
		if (inverter.hasFaults()) {
			inverter._setInverterCurrentStateFault(true);
			return State.ERROR;
		}

		final var now = Instant.now(context.clock);
		if (context.isTimeout(now, this.entryAt)) {
			inverter._setMaxStartTimeout(true);
			return State.ERROR;
		}

		if (inverter.isShutdown()) {
			return State.STOPPED;
		}

		// Trying to switch off
		inverter.setRequestedState(S64201RequestedState.OFF);
		return State.GO_STOPPED;
	}
}
