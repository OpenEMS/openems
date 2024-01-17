package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private static enum BatteryGotoSleepState {
		WAIT_FOR_SLEEP, FINISHED;

	}

	private BatteryGotoSleepState state = BatteryGotoSleepState.WAIT_FOR_SLEEP;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.state = BatteryGotoSleepState.WAIT_FOR_SLEEP;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {

		switch (this.state) {
		case WAIT_FOR_SLEEP: {
			if (!context.isBatteryAwake()) { // If it's already asleep - we're done // TODO: We might get this in an
												// error state
				this.state = BatteryGotoSleepState.FINISHED;
			} else {
				context.setBatteryWakeSleep(false); // if awake - put it to sleep
			}
			break;
		}
		case FINISHED: {
			return State.STOPPED;
		}
		}

		return State.GO_STOPPED;
	}
}