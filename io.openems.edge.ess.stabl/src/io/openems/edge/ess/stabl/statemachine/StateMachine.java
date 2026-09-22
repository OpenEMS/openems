package io.openems.edge.ess.stabl.statemachine;

import io.openems.edge.ess.stabl.EssStablImpl;
import io.openems.edge.ess.stabl.enums.ActivatePowerStage;
import io.openems.edge.ess.stabl.enums.ActualMainState;
import io.openems.edge.ess.stabl.enums.EssState;

public class StateMachine {

	private final Context context;
	private final StartHandler startHandler = new StartHandler();
	private final RunHandler runHandler = new RunHandler();
	private final ErrorHandler errorHandler = new ErrorHandler();
	private final EmergencyHandler emergencyHandler = new EmergencyHandler();
	private final UndefinedHandler undefinedHandler = new UndefinedHandler();

	public StateMachine(EssStablImpl parent) {
		this.context = new Context(parent);
	}

	/**
	 * Runs the state machine for the current cycle.
	 *
	 * @param essState the configured ESS state
	 */
	public void run(EssState essState) {
		switch (essState) {
		case AUTO -> this.runAutoMode();
		case FORCE_TO_STOP -> this.context.getParent().setPowerStage(ActivatePowerStage.POWER_STAGE_OFF.getValue());
		case FORCE_TO_START -> this.context.getParent().setPowerStage(ActivatePowerStage.POWER_STAGE_ON.getValue());
		}
	}

	/**
	 * Returns true if the ESS is currently in RUN state.
	 *
	 * @return true if in RUN state
	 */
	public boolean isRunning() {
		return this.context.getParent().getActualMainState() == ActualMainState.RUN;
	}

	private void runAutoMode() {
		var parent = this.context.getParent();
		var currentState = parent.getActualMainState();

		// Clear error recovery state if system has recovered
		if (this.context.resetAttemptCount > 0 && currentState != ActualMainState.ERROR) {
			this.errorHandler.resetRecoveryState(this.context);
		}

		// Reset start handler pulse when not in a start state,
		// so re-entry into IDLE always triggers a fresh 0→1 edge
		if (currentState != ActualMainState.INIT && currentState != ActualMainState.READY
				&& currentState != ActualMainState.STANDBY && currentState != ActualMainState.IDLE) {
			this.startHandler.reset();
		}

		var powerStage = switch (currentState) {
		case INIT, READY, STANDBY, IDLE -> this.startHandler.handle(this.context);
		case RUN -> this.runHandler.handle(this.context);
		case ERROR -> this.errorHandler.handle(this.context);
		case EMERGENCY -> this.emergencyHandler.handle(this.context);
		case UNDEFINED -> this.undefinedHandler.handle(this.context);
		};

		if (powerStage != null) {
			parent.setPowerStage(powerStage.getValue());
		}
	}

}
