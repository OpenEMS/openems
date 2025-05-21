package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201RequestedState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();
		inverter.setRequestedState(S64201RequestedState.OFF);
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var inverter = context.getParent();
		if (!inverter.hasFailure()) {
			return State.UNDEFINED;
		}
		return State.ERROR;
	}

	@Override
	protected void onExit(Context context) {
		final var inverter = context.getParent();
		inverter._setMaxStartTimeout(false);
		inverter._setMaxStopTimeout(false);
	}
}
