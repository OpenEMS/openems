package io.openems.edge.simulator.meter.grid.acting;

import java.time.Duration;
import java.time.Instant;

public class StepResponseHandler {

	private State state;
	private int repetitionCounter;
	private SimulatorGridMeterActingImpl meter;
	private Instant startTime;

	private static int TIME_THRESHOLD_SECONDS = 120; // [120 seconds in each step]

	public StepResponseHandler(SimulatorGridMeterActingImpl meter, Instant startTime) {
		this.state = State.UNDEFINED;
		// repeats atleast once
		this.repetitionCounter = 1;
		this.meter = meter;
		this.startTime = startTime;
	}

	/**
	 * State Machine for frequency step response.
	 */
	public void doStepResponse() {
		switch (this.state) {
		case UNDEFINED -> this.state = State.INITIAL_FREQ;
		case INITIAL_FREQ -> this.handleStateTransition(50000, State.FIRST_STEPDOWN_FREQUENCY);
		case FIRST_STEPDOWN_FREQUENCY -> this.handleStateTransition(49750, State.SECOND_STEPDOWN_FREQUENCY);
		case SECOND_STEPDOWN_FREQUENCY -> this.handleStateTransition(49650, State.FINISH);
		case FINISH -> {
			if (this.repetitionCounter <= 1) {
				this.handleFinishTransition(50000, State.FINISH);
			} else {
				this.repetitionCounter--;
				this.handleFinishTransition(50000, State.INITIAL_FREQ);
			}
		}
		}
		this.meter._setStateMachine(this.state);
	}

	private void handleStateTransition(int frequency, State nextState) {
		var now = this.meter.getCurrentTime();
		this.meter._setFrequency(frequency);
		if (Duration.between(this.startTime, now).getSeconds() > TIME_THRESHOLD_SECONDS) {
			this.startTime = now;
			this.state = nextState;
		}
	}

	private void handleFinishTransition(int frequency, State nextState) {
		this.meter._setFrequency(frequency);
		this.state = nextState;
	}
}
