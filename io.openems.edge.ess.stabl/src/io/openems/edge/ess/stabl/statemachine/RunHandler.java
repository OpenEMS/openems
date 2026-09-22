package io.openems.edge.ess.stabl.statemachine;

import io.openems.edge.ess.stabl.enums.ActivatePowerStage;

/**
 * Handles RUN state.
 *
 * <p>
 * The system is fully operational. No power stage change is needed here —
 * power is applied via {@code applyPower()} which is only executed in this
 * state.
 */
public class RunHandler {

	public ActivatePowerStage handle(Context context) {
		return null; // No power stage change; applyPower() handles power in RUN state
	}

}
