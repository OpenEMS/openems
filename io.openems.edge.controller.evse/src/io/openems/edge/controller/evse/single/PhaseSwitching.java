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
	 * Phase-Switching automatically adapts based on mode:.
	 * 
	 * <ul>
	 * <li>PV (SURPLUS): Switches between single and three phase depending on
	 * available power.
	 * <li>PV+Min (MINIMUM): Single phase if no PV surplus, otherwise like PV.
	 * <li>Force Charge (FORCE): Three phase with max power.
	 * </ul>
	 */
	AUTOMATIC, //
}