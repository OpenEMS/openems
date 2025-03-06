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
		
		return switch(this.state) {
			case WAIT_FOR_SLEEP -> {
				if (!context.isBatteryAwake()) {
	                this.state = BatteryGotoSleepState.FINISHED;
	                yield State.GO_STOPPED; // Final state after finishing sleep
	            } else {
	                context.setBatteryWakeSleep(false); // Put it to sleep
	                yield State.GO_STOPPED; // Keep processing
	            }
			}
			case FINISHED -> State.STOPPED;
			default -> State.GO_STOPPED;
		};

	}
}