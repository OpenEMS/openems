package io.openems.edge.ess.stabl.statemachine;

import io.openems.edge.ess.stabl.enums.ActivatePowerStage;

/**
 * Handles INIT, READY, STANDBY, and IDLE states.
 *
 * <p>
 * In these states the system is not yet running, so the power stage is
 * activated to allow the ESS to transition to RUN state.
 */
public class StartHandler {

	private boolean offSent = false;

	public ActivatePowerStage handle(Context context) {
		if (!offSent) {
			offSent = true;
			return ActivatePowerStage.POWER_STAGE_OFF; // trigger: send 0 first
		}
		return ActivatePowerStage.POWER_STAGE_ON;      // then send 1 (rising edge)
	}

	void reset() {
		offSent = false;
	}

}
