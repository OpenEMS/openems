package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class Undefined extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		switch (this.getStartStop(context)) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return State.UNDEFINED;

		case START:
			// force START
			if (context.component.hasFaults()) {
				// Has Faults -> error handling
				return State.ERROR_HANDLING;
			} else {
				// No Faults -> start
				return State.GO_RUNNING;
			}

		case STOP:
			// force STOP
			return State.GO_STOPPED;
		}

		assert false;
		return State.UNDEFINED; // can never happen
	}

	/**
	 * Gets the Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @param context the Context
	 * @return {@link StartStop}
	 */
	private StartStop getStartStop(Context context) {
		switch (context.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return context.component.getStartStop();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
	}
}
