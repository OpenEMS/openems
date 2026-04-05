package io.openems.edge.ruhfass.battery.rbti.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.rbti.RuhfassBatteryRbti;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		if (!battery.hasFaults() && context.isStopped()) {
			return State.STOPPED;
		}

		return State.ERROR;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		setValue(battery, RuhfassBatteryRbti.ChannelId.TIMEOUT_START_BATTERY, false);
		setValue(battery, RuhfassBatteryRbti.ChannelId.TIMEOUT_STOP_BATTERY, false);
	}
}