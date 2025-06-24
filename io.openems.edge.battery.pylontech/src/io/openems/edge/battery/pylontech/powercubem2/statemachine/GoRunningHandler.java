package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

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

		return switch (this.state) {
		case WAIT_FOR_WAKE_UP -> {
			if (context.isBatteryAwake()) { // If already awake, we're done
				this.state = BatterySwitchOnState.FINISHED;
				yield State.GO_RUNNING; // Continue to GO_RUNNING
			} else { // If it's not awake, switch it on
				context.setBatteryWakeSleep(true);
				yield State.GO_RUNNING; // Continue to GO_RUNNING
			}
		}
		case FINISHED -> State.RUNNING; // Transition to RUNNING when finished
		default -> State.GO_RUNNING; // Default fallback state
		};
	}

}