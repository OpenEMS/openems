package io.openems.edge.fluidcooling.hydac.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.common.timedata.Timeout;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fluidcooling.hydac.Hydac;
import io.openems.edge.fluidcooling.hydac.statemachine.StateMachine.State;

public class WaitForTargetHandler extends StateHandler<State, Context> {

	protected final Timeout targetUndefinedTimeout = Timeout.ofSeconds(5);

	@Override
	protected void onEntry(Context context) {
		this.targetUndefinedTimeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var hydac = context.getParent();

		if (this.targetUndefinedTimeout.elapsed(context.clock) && hydac.getStartStopTarget() == StartStop.UNDEFINED) {
			setValue(hydac, Hydac.ChannelId.TIMEOUT_TARGET_UNDEFINED, true);
		}

		if (hydac.getStartStopTarget() == StartStop.START) {
			setValue(hydac, Hydac.ChannelId.TIMEOUT_TARGET_UNDEFINED, false);
			return State.RUNNING;
		}

		if (hydac.getStartStopTarget() == StartStop.STOP) {
			setValue(hydac, Hydac.ChannelId.TIMEOUT_TARGET_UNDEFINED, false);
			return State.STOPPED;
		}

		return State.WAIT_FOR_TARGET;
	}
}
