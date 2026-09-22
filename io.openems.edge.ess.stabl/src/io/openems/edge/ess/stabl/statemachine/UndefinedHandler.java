package io.openems.edge.ess.stabl.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.stabl.enums.ActivatePowerStage;

/**
 * Handles UNDEFINED state.
 *
 * <p>
 * No action is taken; the system waits for a defined state.
 */
public class UndefinedHandler {

	private final Logger log = LoggerFactory.getLogger(UndefinedHandler.class);

	public ActivatePowerStage handle(Context context) {
		this.log.warn("Undefined state detected. No action taken.");
		return null;
	}

}
