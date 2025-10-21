package io.openems.edge.ruhfass.battery.rbti.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.TIMEOUT_SECONDS;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Timeout;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.rbti.RuhfassBatteryRbti;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.State;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(TIMEOUT_SECONDS);

	@Override
	protected void onEntry(Context context) {
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		if (battery.hasFaults()) {
			return State.ERROR;
		}
		// Is max allowed start time passed ?
		if (this.timeout.elapsed(context.clock)) {
			setValue(battery, RuhfassBatteryRbti.ChannelId.TIMEOUT_STOP_BATTERY, true);
			return State.ERROR;
		}

		// TODO open contactors
		if (context.isStopped()) {
			return State.STOPPED;
		}
		return State.GO_STOPPED;
	}

}