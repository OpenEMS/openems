package io.openems.edge.controller.evse.single;

public enum PhaseSwitching {
	/**
	 * Phase-Switching is disabled.
	 */
	DISABLE, //
	/**
	 * Phase-Switching forced to SINGLE_PHASE.
	 */
	FORCE_SINGLE_PHASE, //
	/**
	 * Phase-Switching force to THREE_PHASE.
	 */
	FORCE_THREE_PHASE, //
	/**
	 * Phase-Switching in AUTOMATIC mode. (not implemented!).
	 */
	AUTOMATIC_SWITCHING;
}