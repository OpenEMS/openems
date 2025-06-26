package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) {
		this.entryAt = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();
		if (inverter.hasFailure()) {
			return State.ERROR;
		}

		final var now = Instant.now(context.clock);
		if (context.isTimeout(now, this.entryAt)) {
			inverter._setMaxStartTimeout(true);
			return State.ERROR;
		}

		if (inverter.isRunning()) {
			return State.RUNNING;
		}

		// Trying to switch on
		inverter.setRequestedState(S64201RequestedState.GRID_CONNECTED);
		return State.GO_RUNNING;

	}

}
