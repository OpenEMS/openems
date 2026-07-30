package io.openems.edge.batteryinverter.refu.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel.S64800.S64800PcsSetOperation;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(120);

	@Override
	protected void onEntry(Context context) {
		this.timeout.start(context.clock);
		context.getParent()._setMaxStartTimeout(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();
		if (inverter.hasFailure()) {
			return State.ERROR;
		}

		if (this.timeout.elapsed(context.clock)) {
			inverter._setMaxStartTimeout(true);
			return State.ERROR;
		}

		if (inverter.isRunning()) {
			return State.RUNNING;
		}

		inverter.setPcsSetOperation(S64800PcsSetOperation.START_PCS);
		return State.GO_RUNNING;
	}
}