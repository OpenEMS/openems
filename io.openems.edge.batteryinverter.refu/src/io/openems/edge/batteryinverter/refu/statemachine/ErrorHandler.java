package io.openems.edge.batteryinverter.refu.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel.S64800.S64800PcsSetOperation;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();
		inverter.setPcsSetOperation(S64800PcsSetOperation.ENTER_STANDBY_MODE);
		inverter._setStartStop(StartStop.STOP);
		inverter._setActivePower(0);
		inverter._setReactivePower(0);
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
		inverter.setInverterCurrentStateFault(false);
	}
}
