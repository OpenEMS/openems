package io.openems.edge.ess.stabl.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.stabl.enums.ActivatePowerStage;

/**
 * Handles EMERGENCY state.
 *
 * <p>
 * Forces the power stage OFF immediately.
 */
public class EmergencyHandler {

	private final Logger log = LoggerFactory.getLogger(EmergencyHandler.class);

	public ActivatePowerStage handle(Context context) {
		this.log.warn("Emergency state detected. Power stage forced OFF.");
		return ActivatePowerStage.POWER_STAGE_OFF;
	}

}
