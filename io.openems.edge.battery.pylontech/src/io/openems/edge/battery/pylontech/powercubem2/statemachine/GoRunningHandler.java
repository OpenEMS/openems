package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class GoRunningHandler extends StateHandler<State, Context> {

	private static enum BatterySwitchOnState {
		WAIT_FOR_WAKE_UP, FINISHED;

	}

	private BatterySwitchOnState state = BatterySwitchOnState.WAIT_FOR_WAKE_UP;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.state = BatterySwitchOnState.WAIT_FOR_WAKE_UP;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {

		switch (this.state) {
		case WAIT_FOR_WAKE_UP: {
			if (context.isBatteryAwake()) { // If it's already awake (in charge or discharge mode - we are done)
				this.state = BatterySwitchOnState.FINISHED;
			} else { // If its not on, we switch it on
				context.setBatteryWakeSleep(true);
			}
			break;
		}
		case FINISHED: {
			return State.RUNNING;
		}
		}

		return State.GO_RUNNING;
	}

}