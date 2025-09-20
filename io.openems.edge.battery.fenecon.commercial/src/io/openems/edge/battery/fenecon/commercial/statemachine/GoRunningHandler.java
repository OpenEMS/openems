package io.openems.edge.battery.fenecon.commercial.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static enum BatteryRelayState {
		WAIT_FOR_RELAYS_SWITCH_ON, WAIT_FOR_RELAYS_SWITCH_OFF, FINISHED;
	}

	private BatteryRelayState state = BatteryRelayState.WAIT_FOR_RELAYS_SWITCH_ON;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		switch (this.state) {
		case WAIT_FOR_RELAYS_SWITCH_ON: {
			// Master Started
			if (context.isBatteryStarted()) {
				this.state = BatteryRelayState.WAIT_FOR_RELAYS_SWITCH_OFF;

				// Master not started and relay is true
			} else if (context.getBatteryStartStopRelay() == Boolean.TRUE) {
				this.state = BatteryRelayState.WAIT_FOR_RELAYS_SWITCH_OFF;
			} else {
				// Master not started and relay is false, switch the relay on
				context.setBatteryStartUpRelays(true);
			}
		}
			break;
		case WAIT_FOR_RELAYS_SWITCH_OFF: {
			// Master not started and relay is on, switch relay off
			context.setBatteryStartUpRelays(false);

			// Relay is off and master started
			if (context.getBatteryStartStopRelay() != Boolean.TRUE) {
				this.state = BatteryRelayState.FINISHED;
			}
		}
			break;
		case FINISHED:
			if (context.isBatteryStarted()) {
				this.state = BatteryRelayState.WAIT_FOR_RELAYS_SWITCH_ON;
				return State.RUNNING;
			}
			return State.GO_RUNNING;
		}
		return State.GO_RUNNING;
	}
}
