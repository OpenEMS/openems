package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;

public class GoRunning extends StateMachine.Handler {

	private Instant lastStartAttempt = Instant.MIN;
	private int startAttemptCounter = 0;

	@Override
	protected void onExit() {
		this.lastStartAttempt = Instant.MIN;
		this.startAttemptCounter = 0;
	}

	@Override
	public StateMachine getNextState(StateMachine.Context context) {
		if (context.preChargeControl == PreChargeControl.RUNNING) {
			return StateMachine.RUNNING;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastStartAttempt, Instant.now())
				.getSeconds() > context.config.maxStartTime();
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try
			if (this.startAttemptCounter > context.config.maxStartAttempts()) {
				// Too many tries
				// TODO figure out what to do now... possible setting a Fault-Channel, stopping
				// + restarting the system
				System.out.println("Too many tries");
				return StateMachine.GO_ERROR_HANDLING;
			} else {
				// Trying to switch on
				context.setPreChargeControl = PreChargeControl.SWITCH_ON;
				this.lastStartAttempt = Instant.now();
				this.startAttemptCounter++;
				return StateMachine.GO_RUNNING;
			}
		} else {
			// Still waiting...
			return StateMachine.GO_RUNNING;
		}
	}

}
